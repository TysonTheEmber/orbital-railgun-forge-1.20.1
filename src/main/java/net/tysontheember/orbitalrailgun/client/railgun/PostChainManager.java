package net.tysontheember.orbitalrailgun.client.railgun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.ShaderModBridge;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.config.OrbitalRailgunConfig;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class PostChainManager {
    private static final ResourceLocation RAILGUN_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/railgun.json");
    private static final ResourceLocation COMPAT_OVERLAY_ID = ForgeOrbitalRailgunMod.id("shaders/post/compat_overlay.json");
    private static final RenderLevelStageEvent.Stage VANILLA_STAGE = RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
    private static final RenderLevelStageEvent.Stage COMPAT_STAGE = RenderLevelStageEvent.Stage.AFTER_PARTICLES;
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
        ForgeOrbitalRailgunMod.id("strike"),
        ForgeOrbitalRailgunMod.id("gui")
    );
    private static final Field PASSES_FIELD = findPassesField();

    private static Mode mode = Mode.DISABLED;
    private static PostChain railgunChain;
    private static EffectInstance compatOverlay;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    private static boolean shaderPackActive;
    private static boolean resourcesReady;
    private static boolean loggedShaderPack;
    private static ResourceManager lastResourceManager;

    private PostChainManager() {}

    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                reload(resourceManager);
            }
        });
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        boolean active = ShaderModBridge.isShaderPackInUse();
        if (active != shaderPackActive) {
            shaderPackActive = active;
            loggedShaderPack = false;
            updateMode(false);
        }
        if (mode == Mode.POST_CHAIN) {
            resizeChain(minecraft);
        }
    }

    public static void onScreenResize(Minecraft minecraft) {
        if (mode == Mode.POST_CHAIN) {
            resizeChain(minecraft);
        }
    }

    public static boolean shouldHandleStage(RenderLevelStageEvent.Stage stage) {
        if (!isActive()) {
            return false;
        }
        RenderLevelStageEvent.Stage expected = mode == Mode.COMPAT_OVERLAY ? COMPAT_STAGE : VANILLA_STAGE;
        return stage == expected;
    }

    public static boolean isActive() {
        return resourcesReady && mode != Mode.DISABLED;
    }

    public static void prepareFrame(Minecraft minecraft) {
        if (mode == Mode.POST_CHAIN) {
            resizeChain(minecraft);
        }
    }

    public static void render(RenderLevelStageEvent event, Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection,
                               Vec3 cameraPos, Vec3 targetPos, float distance, float timeSeconds, float isBlockHit,
                               boolean strikeActive, RailgunState state) {
        if (!isActive()) {
            return;
        }

        Vec3 safeTarget = targetPos != null ? targetPos : cameraPos;
        float clampedTime = Math.max(timeSeconds, 0.0F);
        float clampedDistance = Math.max(distance, 0.0F);
        float strikeFlag = strikeActive ? 1.0F : 0.0F;
        float selectionFlag = state.isCharging() ? 1.0F : 0.0F;
        int hitKind = state.getHitKind().ordinal();

        if (mode == Mode.POST_CHAIN) {
            applyUniformsToChain(modelView, projection, inverseProjection, cameraPos, safeTarget, clampedDistance,
                clampedTime, isBlockHit, strikeFlag, selectionFlag, hitKind);
            if (railgunChain != null) {
                railgunChain.process(event.getPartialTick());
            }
        } else if (mode == Mode.COMPAT_OVERLAY) {
            applyUniformsToCompat(modelView, projection, inverseProjection, cameraPos, safeTarget, clampedDistance,
                clampedTime, isBlockHit, strikeFlag, selectionFlag, hitKind);
            renderCompatOverlay();
        }
    }

    private static void reload(ResourceManager resourceManager) {
        lastResourceManager = resourceManager;
        ShaderModBridge.reset();
        shaderPackActive = ShaderModBridge.isShaderPackInUse();
        loggedShaderPack = false;
        updateMode(true);
    }

    private static void updateMode(boolean forceReload) {
        Mode desiredMode = computeDesiredMode();
        if (!forceReload && desiredMode == mode) {
            return;
        }
        closeResources();
        mode = desiredMode;
        resourcesReady = false;

        if (mode == Mode.POST_CHAIN) {
            loadPostChain();
        } else if (mode == Mode.COMPAT_OVERLAY) {
            loadCompatOverlay();
        }
    }

    private static Mode computeDesiredMode() {
        boolean forceVanilla = OrbitalRailgunConfig.CLIENT.forceVanillaPostChain.get();
        boolean disableWithShaderpack = OrbitalRailgunConfig.CLIENT.disableWithShaderpack.get();
        if (shaderPackActive && !forceVanilla) {
            if (disableWithShaderpack) {
                return Mode.DISABLED;
            }
            return Mode.COMPAT_OVERLAY;
        }
        return Mode.POST_CHAIN;
    }

    private static void loadPostChain() {
        if (lastResourceManager == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }
        try {
            railgunChain = new PostChain(minecraft.getTextureManager(), lastResourceManager, mainTarget, RAILGUN_CHAIN_ID);
            chainWidth = -1;
            chainHeight = -1;
            resizeChain(minecraft);
            resourcesReady = railgunChain != null;
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun post chain", exception);
            closeChain();
            resourcesReady = false;
        }
    }

    private static void loadCompatOverlay() {
        try {
            compatOverlay = new EffectInstance(COMPAT_OVERLAY_ID);
            resourcesReady = compatOverlay != null;
            logCompatibility();
        } catch (Exception exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun compatibility overlay", exception);
            closeCompatOverlay();
            resourcesReady = false;
        }
    }

    private static void renderCompatOverlay() {
        if (compatOverlay == null) {
            return;
        }
        RenderTarget target = RenderSystem.getMainRenderTarget();
        if (target == null) {
            return;
        }

        RenderSystem.bindTexture(0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(() -> compatOverlay);
        compatOverlay.apply();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(-1.0D, -1.0D, 0.0D).endVertex();
        bufferBuilder.vertex(1.0D, -1.0D, 0.0D).endVertex();
        bufferBuilder.vertex(1.0D, 1.0D, 0.0D).endVertex();
        bufferBuilder.vertex(-1.0D, 1.0D, 0.0D).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        compatOverlay.clear();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void applyUniformsToChain(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection, Vec3 cameraPos,
                                             Vec3 targetPos, float distance, float timeSeconds, float isBlockHit,
                                             float strikeFlag, float selectionFlag, int hitKind) {
        List<PostPass> passes = getPasses();
        if (passes.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget == null) {
            return;
        }
        float width = renderTarget.width > 0 ? renderTarget.width : renderTarget.viewWidth;
        float height = renderTarget.height > 0 ? renderTarget.height : renderTarget.viewHeight;
        for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            if (effect == null) {
                continue;
            }
            ResourceLocation passName = getPassName(pass);
            boolean expectsModelViewMatrix = passName != null && MODEL_VIEW_UNIFORM_PASSES.contains(passName);
            setMatrix(effect, "ProjMat", projection);
            if (expectsModelViewMatrix) {
                setMatrix(effect, "ModelViewMat", modelView);
            }
            setMatrix(effect, "InverseTransformMatrix", inverseProjection);
            setVec3(effect, "CameraPosition", cameraPos);
            setVec3(effect, "BlockPosition", targetPos);
            setVec3(effect, "HitPos", targetPos);
            setVec2(effect, "OutSize", width, height);
            setFloat(effect, "iTime", timeSeconds);
            setFloat(effect, "Distance", distance);
            setFloat(effect, "IsBlockHit", isBlockHit);
            setFloat(effect, "StrikeActive", strikeFlag);
            setFloat(effect, "SelectionActive", selectionFlag);
            setInt(effect, "HitKind", hitKind);
        }
    }

    private static void applyUniformsToCompat(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection, Vec3 cameraPos,
                                              Vec3 targetPos, float distance, float timeSeconds, float isBlockHit,
                                              float strikeFlag, float selectionFlag, int hitKind) {
        if (compatOverlay == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget == null) {
            return;
        }
        float width = renderTarget.width > 0 ? renderTarget.width : renderTarget.viewWidth;
        float height = renderTarget.height > 0 ? renderTarget.height : renderTarget.viewHeight;
        setMatrix(compatOverlay, "ProjMat", projection);
        setMatrix(compatOverlay, "ModelViewMat", modelView);
        setMatrix(compatOverlay, "InverseTransformMatrix", inverseProjection);
        setVec3(compatOverlay, "CameraPosition", cameraPos);
        setVec3(compatOverlay, "BlockPosition", targetPos);
        setVec3(compatOverlay, "HitPos", targetPos);
        setVec2(compatOverlay, "OutSize", width, height);
        setFloat(compatOverlay, "iTime", timeSeconds);
        setFloat(compatOverlay, "Distance", distance);
        setFloat(compatOverlay, "IsBlockHit", isBlockHit);
        setFloat(compatOverlay, "StrikeActive", strikeFlag);
        setFloat(compatOverlay, "SelectionActive", selectionFlag);
        setInt(compatOverlay, "HitKind", hitKind);
    }

    private static void resizeChain(Minecraft minecraft) {
        if (railgunChain == null) {
            return;
        }
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width == chainWidth && height == chainHeight) {
            return;
        }
        railgunChain.resize(width, height);
        chainWidth = width;
        chainHeight = height;
    }

    private static void closeResources() {
        closeChain();
        closeCompatOverlay();
    }

    private static void closeChain() {
        if (railgunChain != null) {
            try {
                railgunChain.close();
            } catch (Exception ignored) {
            }
            railgunChain = null;
        }
        chainWidth = -1;
        chainHeight = -1;
    }

    private static void closeCompatOverlay() {
        if (compatOverlay != null) {
            try {
                compatOverlay.close();
            } catch (Exception ignored) {
            }
            compatOverlay = null;
        }
    }

    private static List<PostPass> getPasses() {
        if (railgunChain == null) {
            return Collections.emptyList();
        }
        if (PASSES_FIELD == null) {
            return Collections.emptyList();
        }
        try {
            Object value = PASSES_FIELD.get(railgunChain);
            if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<PostPass> passes = (List<PostPass>) list;
                return passes;
            }
            ForgeOrbitalRailgunMod.LOGGER.error("Orbital railgun post chain passes had unexpected type: {}",
                value == null ? "null" : value.getClass().getName());
        } catch (IllegalAccessException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to access orbital railgun post chain passes", exception);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private static ResourceLocation getPassName(PostPass pass) {
        String name = pass.getName();
        return name != null ? ResourceLocation.tryParse(name) : null;
    }

    private static Field findPassesField() {
        try {
            Field field = PostChain.class.getDeclaredField("passes");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            try {
                Field field = PostChain.class.getDeclaredField("f_110009_");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException exception) {
                ForgeOrbitalRailgunMod.LOGGER.error(
                    "Unable to find passes field on PostChain using Mojmap or SRG identifiers",
                    exception
                );
                return null;
            }
        }
    }

    private static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }

    private static void setVec2(EffectInstance effect, String name, float x, float y) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setInt(EffectInstance effect, String name, int value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void logCompatibility() {
        if (!OrbitalRailgunConfig.CLIENT.logIrisState.get()) {
            return;
        }
        if (!shaderPackActive) {
            return;
        }
        if (loggedShaderPack) {
            return;
        }
        ForgeOrbitalRailgunMod.LOGGER.info("[orbital_railgun] Shader pack detected: enabling compatibility mode");
        loggedShaderPack = true;
    }

    private enum Mode {
        DISABLED,
        POST_CHAIN,
        COMPAT_OVERLAY
    }
}

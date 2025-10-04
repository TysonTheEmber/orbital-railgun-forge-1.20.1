package net.tysontheember.orbitalrailgun.client.railgun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.ShaderModBridge;
import net.tysontheember.orbitalrailgun.config.OrbitalRailgunClientConfig;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class PostChainManager {
    private static final ResourceLocation RAILGUN_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/railgun.json");
    private static final ResourceLocation COMPAT_OVERLAY_ID = ForgeOrbitalRailgunMod.id("shaders/post/compat_overlay.json");
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
        ForgeOrbitalRailgunMod.id("strike"),
        ForgeOrbitalRailgunMod.id("gui")
    );

    private static final Field PASSES_FIELD = findPassesField();

    private static PostChain railgunChain;
    private static EffectInstance compatOverlay;
    private static boolean chainReady;
    private static boolean overlayReady;
    private static boolean compatibilityMode;
    private static boolean disabledForShaderPack;
    private static boolean shaderPackActive;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    private static ResourceManager lastResourceManager;

    static {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PostChainManager::onRegisterReloadListeners);
    }

    private PostChainManager() {
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
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

    public static void ensureState() {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resourceManager = lastResourceManager != null ? lastResourceManager : minecraft.getResourceManager();
        if (resourceManager == null) {
            return;
        }

        boolean shaderInUse = ShaderModBridge.isShaderPackInUse();
        boolean disableWithShader = OrbitalRailgunClientConfig.CLIENT.compat.disableWithShaderpack.get();
        boolean forceVanilla = OrbitalRailgunClientConfig.CLIENT.compat.forceVanillaPostChain.get();

        boolean shouldCompatibility = shaderInUse && !forceVanilla && !disableWithShader;
        boolean shouldDisable = shaderInUse && disableWithShader;

        if (shaderInUse != shaderPackActive || shouldCompatibility != compatibilityMode || shouldDisable != disabledForShaderPack) {
            reload(resourceManager);
        }
    }

    private static void reload(ResourceManager resourceManager) {
        Minecraft minecraft = Minecraft.getInstance();
        lastResourceManager = resourceManager;
        closeResources();

        shaderPackActive = ShaderModBridge.isShaderPackInUse();
        boolean disableWithShader = OrbitalRailgunClientConfig.CLIENT.compat.disableWithShaderpack.get();
        boolean forceVanilla = OrbitalRailgunClientConfig.CLIENT.compat.forceVanillaPostChain.get();

        compatibilityMode = shaderPackActive && !forceVanilla && !disableWithShader;
        disabledForShaderPack = shaderPackActive && disableWithShader;

        if (OrbitalRailgunClientConfig.CLIENT.compat.logIrisState.get() && ShaderModBridge.isShaderModPresent()) {
            if (compatibilityMode) {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected: enabling compatibility mode");
            } else if (disabledForShaderPack) {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected: disabling orbital railgun post effects");
            } else if (shaderPackActive && forceVanilla) {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected: forcing vanilla orbital railgun post chain");
            } else {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack compatibility not required");
            }
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            chainReady = false;
            overlayReady = false;
            return;
        }

        if (disabledForShaderPack) {
            return;
        }

        if (compatibilityMode) {
            try {
                compatOverlay = new EffectInstance(COMPAT_OVERLAY_ID);
                overlayReady = true;
            } catch (IOException exception) {
                overlayReady = false;
                compatOverlay = null;
                ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun compatibility overlay", exception);
            }
            return;
        }

        try {
            railgunChain = new PostChain(minecraft.getTextureManager(), resourceManager, mainTarget, RAILGUN_CHAIN_ID);
            chainReady = true;
            chainWidth = -1;
            chainHeight = -1;
            resizeChain(minecraft);
        } catch (IOException exception) {
            chainReady = false;
            railgunChain = null;
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun post chain", exception);
        }
    }

    public static boolean isStageValid(RenderLevelStageEvent.Stage stage) {
        if (compatibilityMode) {
            return stage == RenderLevelStageEvent.Stage.AFTER_WEATHER;
        }
        return stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
    }

    public static boolean isReady() {
        if (disabledForShaderPack) {
            return false;
        }
        return compatibilityMode ? overlayReady : chainReady && railgunChain != null;
    }

    public static void resizeIfNeeded() {
        if (compatibilityMode || railgunChain == null) {
            return;
        }
        resizeChain(Minecraft.getInstance());
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

    public static void render(RenderLevelStageEvent event, RailgunState state, boolean strikeActive, boolean chargeActive,
                              float timeSeconds, Matrix4f projection, Matrix4f inverseProjection, Matrix4f modelView,
                              Vec3 cameraPos, Vec3 targetPos, float distance, float isBlockHit) {
        if (!isReady()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }

        float width = mainTarget.width > 0 ? mainTarget.width : mainTarget.viewWidth;
        float height = mainTarget.height > 0 ? mainTarget.height : mainTarget.viewHeight;

        if (compatibilityMode) {
            if (!overlayReady || compatOverlay == null) {
                return;
            }
            renderCompatibilityOverlay(mainTarget, width, height, timeSeconds, state, strikeActive, isBlockHit, distance,
                projection, inverseProjection, modelView, cameraPos, targetPos);
            return;
        }

        resizeChain(minecraft);
        applyUniformsToPasses(projection, inverseProjection, modelView, cameraPos, targetPos, width, height,
            timeSeconds, distance, isBlockHit, strikeActive, chargeActive, state);
        railgunChain.process(event.getPartialTick());
    }

    private static void renderCompatibilityOverlay(RenderTarget mainTarget, float width, float height, float timeSeconds,
                                                   RailgunState state, boolean strikeActive, float isBlockHit,
                                                   float distance, Matrix4f projection, Matrix4f inverseProjection,
                                                   Matrix4f modelView, Vec3 cameraPos, Vec3 targetPos) {
        compatOverlay.setSampler("DiffuseSampler", mainTarget::getColorTextureId);
        Matrix4f identity = new Matrix4f().identity();
        setMatrix(compatOverlay, "ProjMat", identity);
        setMatrix(compatOverlay, "ModelViewMat", identity);
        setMatrix(compatOverlay, "InverseTransformMatrix", inverseProjection);
        setVec3(compatOverlay, "CameraPosition", cameraPos);
        setVec3(compatOverlay, "BlockPosition", targetPos);
        setVec3(compatOverlay, "HitPos", targetPos);
        setVec2(compatOverlay, "OutSize", width, height);
        setFloat(compatOverlay, "iTime", clampTime(timeSeconds));
        setFloat(compatOverlay, "Distance", distance);
        setFloat(compatOverlay, "IsBlockHit", isBlockHit);
        setFloat(compatOverlay, "StrikeActive", strikeActive ? 1.0F : 0.0F);
        setFloat(compatOverlay, "SelectionActive", state.isCharging() ? 1.0F : 0.0F);
        setInt(compatOverlay, "HitKind", state.getHitKind().ordinal());

        RenderSystem.bindTexture(0);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        compatOverlay.apply();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
        builder.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
        builder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        compatOverlay.clear();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static float clampTime(float timeSeconds) {
        return Math.max(0.0F, Math.min(timeSeconds, 120.0F));
    }

    private static void applyUniformsToPasses(Matrix4f projection, Matrix4f inverseProjection, Matrix4f modelView,
                                              Vec3 cameraPos, Vec3 targetPos, float width, float height, float timeSeconds,
                                              float distance, float isBlockHit, boolean strikeActive, boolean chargeActive,
                                              RailgunState state) {
        List<PostPass> passes = getPasses();
        if (passes.isEmpty()) {
            return;
        }

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
            setFloat(effect, "StrikeActive", strikeActive ? 1.0F : 0.0F);
            setFloat(effect, "SelectionActive", chargeActive ? 1.0F : 0.0F);
            setInt(effect, "HitKind", state.getHitKind().ordinal());
        }
    }

    private static ResourceLocation getPassName(PostPass pass) {
        String name = pass.getName();
        return name != null ? ResourceLocation.tryParse(name) : null;
    }

    private static List<PostPass> getPasses() {
        if (railgunChain == null || PASSES_FIELD == null) {
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
        }
        return Collections.emptyList();
    }

    private static Field findPassesField() {
        try {
            Field field = ObfuscationReflectionHelper.findField(PostChain.class, "passes");
            field.setAccessible(true);
            return field;
        } catch (ObfuscationReflectionHelper.UnableToFindFieldException ignored) {
            try {
                Field field = ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");
                field.setAccessible(true);
                return field;
            } catch (ObfuscationReflectionHelper.UnableToFindFieldException exception) {
                ForgeOrbitalRailgunMod.LOGGER.error(
                    "Unable to find passes field on PostChain using Mojmap or SRG identifiers",
                    exception
                );
                return null;
            }
        }
    }

    private static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        if (effect == null) {
            return;
        }
        if (matrix == null) {
            return;
        }
        if (effect.getUniform(name) != null) {
            effect.getUniform(name).set(matrix);
        }
    }

    private static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        if (effect == null || vec == null || effect.getUniform(name) == null) {
            return;
        }
        effect.getUniform(name).set((float) vec.x, (float) vec.y, (float) vec.z);
    }

    private static void setVec2(EffectInstance effect, String name, float x, float y) {
        if (effect == null || effect.getUniform(name) == null) {
            return;
        }
        effect.getUniform(name).set(x, y);
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        if (effect == null || effect.getUniform(name) == null) {
            return;
        }
        effect.getUniform(name).set(value);
    }

    private static void setInt(EffectInstance effect, String name, int value) {
        if (effect == null || effect.getUniform(name) == null) {
            return;
        }
        effect.getUniform(name).set(value);
    }

    private static void closeResources() {
        if (railgunChain != null) {
            try {
                railgunChain.close();
            } catch (Exception ignored) {
            }
            railgunChain = null;
        }
        if (compatOverlay != null) {
            try {
                compatOverlay.close();
            } catch (Exception ignored) {
            }
            compatOverlay = null;
        }
        chainReady = false;
        overlayReady = false;
        chainWidth = -1;
        chainHeight = -1;
    }
}

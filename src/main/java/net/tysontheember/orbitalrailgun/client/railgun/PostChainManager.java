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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
    private static final Field PASSES_FIELD = findPassesField();
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
        ForgeOrbitalRailgunMod.id("strike"),
        ForgeOrbitalRailgunMod.id("gui")
    );

    private static PostChain railgunChain;
    private static EffectInstance compatOverlay;
    private static ResourceManager resourceManager;

    private static boolean chainReady;
    private static boolean overlayReady;
    private static boolean compatibilityMode;
    private static boolean shaderPackActive;
    private static boolean shaderModPresent;
    private static boolean renderSuppressed;
    private static boolean loggedCompatMessage;

    private static boolean disableWithShaderpack;
    private static boolean logIrisState;
    private static boolean forceVanillaPostChain;

    private static int chainWidth = -1;
    private static int chainHeight = -1;

    private PostChainManager() {}

    public static void reload(ResourceManager manager) {
        resourceManager = manager;
        disableWithShaderpack = OrbitalRailgunClientConfig.CLIENT.disableWithShaderpack.get();
        logIrisState = OrbitalRailgunClientConfig.CLIENT.logIrisState.get();
        forceVanillaPostChain = OrbitalRailgunClientConfig.CLIENT.forceVanillaPostChain.get();
        shaderModPresent = ShaderModBridge.isShaderModPresent();
        shaderPackActive = shaderModPresent && ShaderModBridge.isShaderPackInUse();
        rebuildResources();
    }

    public static boolean prepareFrame(Minecraft minecraft) {
        if (minecraft == null) {
            return false;
        }
        updateShaderState();
        if (renderSuppressed) {
            return false;
        }
        if (compatibilityMode) {
            return overlayReady && compatOverlay != null;
        }
        if (!chainReady || railgunChain == null) {
            return false;
        }
        resizeChain(minecraft);
        return chainReady;
    }

    public static void render(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection, Vec3 cameraPos, Vec3 targetPos,
                               float distance, float timeSeconds, float isBlockHit, boolean strikeActive, RailgunState state,
                               float partialTick) {
        if (renderSuppressed) {
            return;
        }
        if (compatibilityMode) {
            if (!overlayReady || compatOverlay == null) {
                return;
            }
            applyOverlayUniforms(modelView, projection, inverseProjection, cameraPos, targetPos, distance, timeSeconds,
                isBlockHit, strikeActive, state);
            renderCompatOverlay();
            return;
        }

        if (!chainReady || railgunChain == null) {
            return;
        }
        applyPostChainUniforms(modelView, projection, inverseProjection, cameraPos, targetPos, distance, timeSeconds,
            isBlockHit, strikeActive, state);
        railgunChain.process(partialTick);
    }

    public static void close() {
        closeChain();
        closeOverlay();
    }

    private static void updateShaderState() {
        if (!shaderModPresent) {
            compatibilityMode = false;
            renderSuppressed = false;
            return;
        }
        boolean newState = ShaderModBridge.isShaderPackInUse();
        if (newState != shaderPackActive) {
            shaderPackActive = newState;
            rebuildResources();
        }
    }

    private static void rebuildResources() {
        closeChain();
        closeOverlay();
        loggedCompatMessage = false;

        renderSuppressed = shaderPackActive && disableWithShaderpack;
        compatibilityMode = shaderPackActive && !forceVanillaPostChain && !renderSuppressed;

        if (renderSuppressed) {
            return;
        }

        if (compatibilityMode) {
            loadCompatOverlay();
            if (compatibilityMode && logIrisState) {
                ForgeOrbitalRailgunMod.LOGGER.info("[{}] Shader pack detected: enabling compatibility mode", ForgeOrbitalRailgunMod.MOD_ID);
                loggedCompatMessage = true;
            }
        } else {
            loadPostChain();
            if (!shaderPackActive) {
                loggedCompatMessage = false;
            } else if (logIrisState && !loggedCompatMessage) {
                ForgeOrbitalRailgunMod.LOGGER.info("[{}] Shader pack detected but forcing vanilla post chain", ForgeOrbitalRailgunMod.MOD_ID);
                loggedCompatMessage = true;
            }
        }
    }

    private static void loadPostChain() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }
        if (resourceManager == null) {
            resourceManager = minecraft.getResourceManager();
        }
        try {
            railgunChain = new PostChain(minecraft.getTextureManager(), resourceManager, mainTarget, RAILGUN_CHAIN_ID);
            chainReady = true;
            chainWidth = -1;
            chainHeight = -1;
            resizeChain(minecraft);
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun post chain", exception);
            chainReady = false;
            closeChain();
        }
    }

    private static void loadCompatOverlay() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        ResourceManager manager = resourceManager != null ? resourceManager : minecraft.getResourceManager();
        if (manager == null) {
            return;
        }
        try {
            compatOverlay = new EffectInstance(manager, COMPAT_OVERLAY_ID.toString());
            overlayReady = true;
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun compatibility overlay", exception);
            overlayReady = false;
            closeOverlay();
        }
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

    private static void applyPostChainUniforms(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection,
                                               Vec3 cameraPos, Vec3 targetPos, float distance, float timeSeconds,
                                               float isBlockHit, boolean strikeActive, RailgunState state) {
        List<PostPass> passes = getPasses();
        if (passes.isEmpty()) {
            return;
        }

        RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
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
            setFloat(effect, "StrikeActive", strikeActive ? 1.0F : 0.0F);
            setFloat(effect, "SelectionActive", state.isCharging() ? 1.0F : 0.0F);
            setInt(effect, "HitKind", state.getHitKind().ordinal());
        }
    }

    private static void applyOverlayUniforms(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection,
                                             Vec3 cameraPos, Vec3 targetPos, float distance, float timeSeconds,
                                             float isBlockHit, boolean strikeActive, RailgunState state) {
        if (compatOverlay == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null) {
            return;
        }

        float width = target.width > 0 ? target.width : target.viewWidth;
        float height = target.height > 0 ? target.height : target.viewHeight;

        float clampedTime = Math.max(0.0F, timeSeconds);
        float clampedDistance = Mth.clamp(distance, 0.0F, 2048.0F);
        float clampedIsBlockHit = Mth.clamp(isBlockHit, 0.0F, 1.0F);

        setMatrix(compatOverlay, "ProjMat", projection);
        setMatrix(compatOverlay, "ModelViewMat", modelView);
        setMatrix(compatOverlay, "InverseTransformMatrix", inverseProjection);
        setVec3(compatOverlay, "CameraPosition", cameraPos);
        setVec3(compatOverlay, "BlockPosition", targetPos);
        setVec3(compatOverlay, "HitPos", targetPos);
        setVec2(compatOverlay, "OutSize", width, height);
        setFloat(compatOverlay, "iTime", clampedTime);
        setFloat(compatOverlay, "Distance", clampedDistance);
        setFloat(compatOverlay, "IsBlockHit", clampedIsBlockHit);
        setFloat(compatOverlay, "StrikeActive", strikeActive ? 1.0F : 0.0F);
        setFloat(compatOverlay, "SelectionActive", state.isCharging() ? 1.0F : 0.0F);
        setInt(compatOverlay, "HitKind", state.getHitKind().ordinal());
    }

    private static void renderCompatOverlay() {
        if (compatOverlay == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null) {
            return;
        }

        RenderSystem.bindTexture(0);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        try {
            compatOverlay.apply();

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
            builder.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
            builder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
            builder.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(builder.end());
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void closeChain() {
        if (railgunChain != null) {
            try {
                railgunChain.close();
            } catch (Exception ignored) {
            }
            railgunChain = null;
        }
        chainReady = false;
        chainWidth = -1;
        chainHeight = -1;
    }

    private static void closeOverlay() {
        if (compatOverlay != null) {
            try {
                compatOverlay.close();
            } catch (Exception ignored) {
            }
            compatOverlay = null;
        }
        overlayReady = false;
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
            return ObfuscationReflectionHelper.findField(PostChain.class, "passes");
        } catch (ObfuscationReflectionHelper.UnableToFindFieldException ignored) {
            try {
                return ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");
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
        var uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        if (effect == null || vec == null) {
            return;
        }
        var uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }

    private static void setVec2(EffectInstance effect, String name, float x, float y) {
        if (effect == null) {
            return;
        }
        var uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        if (effect == null) {
            return;
        }
        var uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setInt(EffectInstance effect, String name, int value) {
        if (effect == null) {
            return;
        }
        var uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}

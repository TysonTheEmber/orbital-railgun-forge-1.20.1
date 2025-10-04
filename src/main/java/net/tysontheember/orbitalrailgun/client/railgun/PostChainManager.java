package net.tysontheember.orbitalrailgun.client.railgun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.ShaderModBridge;
import net.tysontheember.orbitalrailgun.config.OrbitalRailgunConfig;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class PostChainManager {
    private static final ResourceLocation RAILGUN_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/railgun.json");
    private static final ResourceLocation COMPAT_OVERLAY_ID = ForgeOrbitalRailgunMod.id("shaders/post/compat_overlay.json");
    private static final Field PASSES_FIELD = findPassesField();
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
        ForgeOrbitalRailgunMod.id("strike"),
        ForgeOrbitalRailgunMod.id("gui"),
        ForgeOrbitalRailgunMod.id("compat_overlay")
    );

    private static PostChain railgunChain;
    private static EffectInstance compatOverlay;

    private static boolean chainReady;
    private static boolean compatReady;
    private static boolean compatMode;
    private static boolean shaderPackActive;
    private static boolean disabledByConfig;
    private static boolean loggedState;

    private static int chainWidth = -1;
    private static int chainHeight = -1;

    private static ResourceManager lastResourceManager;

    private PostChainManager() {}

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PostChainManager::registerReloadListener);
    }

    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                lastResourceManager = resourceManager;
                rebuild(Minecraft.getInstance());
            }
        });
    }

    public static void tick(Minecraft minecraft) {
        boolean currentShaderState = ShaderModBridge.isShaderPackInUse();
        boolean desiredCompat = shouldUseCompat(currentShaderState);
        boolean shouldDisable = currentShaderState && OrbitalRailgunConfig.CLIENT.disableWithShaderpack.get();
        if (currentShaderState != shaderPackActive || desiredCompat != compatMode || shouldDisable != disabledByConfig) {
            shaderPackActive = currentShaderState;
            disabledByConfig = shouldDisable;
            compatMode = desiredCompat;
            rebuild(minecraft);
        }
    }

    public static void onResize() {
        if (!compatMode) {
            resizeChain(Minecraft.getInstance());
        }
    }

    public static void render(RenderLevelStageEvent event, RailgunState state) {
        if (disabledByConfig) {
            return;
        }
        RenderLevelStageEvent.Stage stage = compatMode ? RenderLevelStageEvent.Stage.AFTER_WEATHER : RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
        if (event.getStage() != stage) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (!ensureReady(minecraft)) {
            return;
        }

        Level level = minecraft.level;
        boolean strikeActive = state.isStrikeActive() && state.getStrikeDimension() != null && state.getStrikeDimension().equals(level.dimension());
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
        }

        RenderTarget mainTarget = RenderSystem.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }

        float width = mainTarget.width > 0 ? mainTarget.width : mainTarget.viewWidth;
        float height = mainTarget.height > 0 ? mainTarget.height : mainTarget.viewHeight;

        float timeSeconds = strikeActive
            ? state.getStrikeSeconds(event.getPartialTick())
            : state.getChargeSeconds(event.getPartialTick());

        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4f inverseProjection = new Matrix4f(projection).invert();
        Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose());
        Vec3 cameraPos = event.getCamera().getPosition();

        Vec3 targetPos = strikeActive ? state.getStrikePos() : state.getHitPos();
        float distance = strikeActive
            ? (float) cameraPos.distanceTo(state.getStrikePos())
            : state.getHitDistance();
        float isBlockHit = state.getHitKind() != RailgunState.HitKind.NONE ? 1.0F : 0.0F;

        applyUniforms(modelView, projection, inverseProjection, cameraPos, targetPos, distance, timeSeconds, isBlockHit, strikeActive, state, width, height);
        maybeLogState();

        if (compatMode) {
            renderCompatOverlay(mainTarget, width, height);
        } else if (railgunChain != null) {
            railgunChain.process(event.getPartialTick());
        }
    }

    private static boolean ensureReady(Minecraft minecraft) {
        if (compatMode) {
            return compatReady && compatOverlay != null;
        }
        if (!chainReady || railgunChain == null) {
            return false;
        }
        resizeChain(minecraft);
        return true;
    }

    private static void rebuild(Minecraft minecraft) {
        closeChain();
        closeOverlay();

        if (minecraft == null) {
            return;
        }
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            chainReady = false;
            compatReady = false;
            return;
        }

        shaderPackActive = ShaderModBridge.isShaderPackInUse();
        boolean forceVanilla = OrbitalRailgunConfig.CLIENT.forceVanillaPostChain.get();
        disabledByConfig = shaderPackActive && OrbitalRailgunConfig.CLIENT.disableWithShaderpack.get();
        compatMode = shouldUseCompat(shaderPackActive) && !disabledByConfig && !forceVanilla;
        loggedState = false;

        ResourceManager resourceManager = lastResourceManager != null ? lastResourceManager : minecraft.getResourceManager();

        if (compatMode) {
            try {
                compatOverlay = new EffectInstance(COMPAT_OVERLAY_ID);
                compatReady = true;
            } catch (IOException exception) {
                ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun compatibility overlay", exception);
                compatOverlay = null;
                compatReady = false;
            }
        } else if (!disabledByConfig || !shaderPackActive) {
            try {
                railgunChain = new PostChain(minecraft.getTextureManager(), resourceManager, minecraft.getMainRenderTarget(), RAILGUN_CHAIN_ID);
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

        maybeLogState();
    }

    private static void renderCompatOverlay(RenderTarget mainTarget, float width, float height) {
        if (!compatReady || compatOverlay == null) {
            return;
        }

        mainTarget.bindWrite(false);

        compatOverlay.setSampler("DiffuseSampler", () -> mainTarget.getColorTextureId());
        if (mainTarget.getDepthTextureId() >= 0) {
            compatOverlay.setSampler("DepthSampler", () -> mainTarget.getDepthTextureId());
        }

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA.value, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value, GlStateManager.SourceFactor.ONE.value, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value);
        RenderSystem.bindTexture(0);
        compatOverlay.apply();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(0.0D, height, 0.0D).uv(0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(width, height, 0.0D).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(width, 0.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
        Tesselator.getInstance().end();
        compatOverlay.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void applyUniforms(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection, Vec3 cameraPos, Vec3 targetPos,
                                      float distance, float timeSeconds, float isBlockHit, boolean strikeActive, RailgunState state,
                                      float width, float height) {
        List<EffectInstance> effects = getActiveEffects();
        if (effects.isEmpty()) {
            return;
        }

        for (EffectInstance effect : effects) {
            if (effect == null) {
                continue;
            }
            ResourceLocation passName = getEffectName(effect);
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

    private static ResourceLocation getEffectName(EffectInstance effect) {
        String name = effect.getName();
        return name != null ? ResourceLocation.tryParse(name) : null;
    }

    private static List<EffectInstance> getActiveEffects() {
        if (compatMode) {
            return compatOverlay != null ? List.of(compatOverlay) : Collections.emptyList();
        }
        List<PostPass> passes = getPasses();
        if (passes.isEmpty()) {
            return Collections.emptyList();
        }
        List<EffectInstance> effects = new ArrayList<>(passes.size());
        for (PostPass pass : passes) {
            effects.add(pass.getEffect());
        }
        return effects;
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
            ForgeOrbitalRailgunMod.LOGGER.error("Orbital railgun post chain passes had unexpected type: {}", value == null ? "null" : value.getClass().getName());
        } catch (IllegalAccessException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to access orbital railgun post chain passes", exception);
        }
        return Collections.emptyList();
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

    private static Field findPassesField() {
        try {
            return ObfuscationReflectionHelper.findField(PostChain.class, "passes");
        } catch (ObfuscationReflectionHelper.UnableToFindFieldException ignored) {
            try {
                return ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");
            } catch (ObfuscationReflectionHelper.UnableToFindFieldException exception) {
                ForgeOrbitalRailgunMod.LOGGER.error("Unable to find passes field on PostChain using Mojmap or SRG identifiers", exception);
                return null;
            }
        }
    }

    private static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        if (effect == null) {
            return;
        }
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        if (effect == null) {
            return;
        }
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }

    private static void setVec2(EffectInstance effect, String name, float x, float y) {
        if (effect == null) {
            return;
        }
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        if (effect == null) {
            return;
        }
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setInt(EffectInstance effect, String name, int value) {
        if (effect == null) {
            return;
        }
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
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
        compatReady = false;
    }

    private static boolean shouldUseCompat(boolean shaderActive) {
        if (!shaderActive) {
            return false;
        }
        if (!ShaderModBridge.isShaderModPresent()) {
            return false;
        }
        if (OrbitalRailgunConfig.CLIENT.forceVanillaPostChain.get()) {
            return false;
        }
        if (OrbitalRailgunConfig.CLIENT.disableWithShaderpack.get()) {
            return false;
        }
        return true;
    }

    private static void maybeLogState() {
        if (!OrbitalRailgunConfig.CLIENT.logIrisState.get() || loggedState) {
            return;
        }
        if (shaderPackActive) {
            if (disabledByConfig) {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected: orbital railgun effects disabled by configuration");
            } else if (compatMode && compatReady) {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected: enabling compatibility mode");
            } else if (compatMode) {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected but compatibility overlay failed to initialize");
            } else {
                ForgeOrbitalRailgunMod.LOGGER.info("Shader pack detected but running vanilla post chain");
            }
        } else if (!compatMode) {
            ForgeOrbitalRailgunMod.LOGGER.info("No shader pack detected: running vanilla post chain");
        }
        loggedState = true;
    }
}

package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.compat.OculusCompat;
import net.tysontheember.orbitalrailgun.client.config.ClientConfig;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.item.OrbitalRailgunItem;
import net.tysontheember.orbitalrailgun.network.C2S_RequestFire;
import net.tysontheember.orbitalrailgun.network.Network;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation RAILGUN_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/railgun.json");
    private static final ResourceLocation COMPAT_VIGNETTE_TEX = ForgeOrbitalRailgunMod.id("textures/gui/compat_vignette.png");
    private static final ResourceLocation COMPAT_OVERLAY_PROGRAM = ForgeOrbitalRailgunMod.id("compat_overlay");
    private static final Field PASSES_FIELD = findPassesField();
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
            ForgeOrbitalRailgunMod.id("strike"),
            ForgeOrbitalRailgunMod.id("gui")
    );

    private static PostChain railgunChain;
    private static boolean chainReady;
    private static int chainWidth = -1;
    private static int chainHeight = -1;

    private static boolean compatModeActive;
    private static boolean compatOverlayEnabled;
    private static boolean hasCompatVignette;
    private static EffectInstance compatOverlayEffect;
    private static long compatOverlayStartMs;
    private static ResourceKey<Level> lastLoggedWorld;
    private static boolean attackWasDown;

    private static boolean pendingCompatModeActive;
    private static boolean pendingCompatOverlayEnabled;
    private static boolean pendingShaderpackActive;
    private static boolean pendingHasCompatVignette;

    static {
        if (PASSES_FIELD != null) {
            PASSES_FIELD.setAccessible(true);
        } else {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to locate orbital railgun post chain passes field");
        }
    }

    private ClientEvents() {}

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                onReloadPrepare(resourceManager, profiler);
                return null;
            }

            @Override
            protected void apply(Void ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
                onReloadApply(resourceManager, profiler);
            }
        });
    }

    public static void onReloadPrepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        pendingHasCompatVignette = resourceManager.getResource(COMPAT_VIGNETTE_TEX).isPresent();
        pendingShaderpackActive = OculusCompat.isShaderpackActive();
        pendingCompatModeActive = shouldUseCompat(pendingShaderpackActive);
        pendingCompatOverlayEnabled = shouldDrawCompatOverlay(pendingShaderpackActive, pendingCompatModeActive);
    }

    public static void onReloadApply(ResourceManager resourceManager, ProfilerFiller profiler) {
        Minecraft minecraft = Minecraft.getInstance();
        closeChain();

        hasCompatVignette = pendingHasCompatVignette;
        compatModeActive = pendingCompatModeActive;
        compatOverlayEnabled = pendingCompatOverlayEnabled;

        logShaderpackState("reload", pendingShaderpackActive, compatModeActive, compatOverlayEnabled);

        if (minecraft.getMainRenderTarget() == null) {
            chainReady = false;
            return;
        }

        if (compatModeActive) {
            if (compatOverlayEnabled) {
                loadCompatOverlay(resourceManager);
            } else {
                closeCompatOverlay();
            }
            return;
        }

        closeCompatOverlay();
        loadChain(minecraft, resourceManager);
    }

    static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!chainReady || railgunChain == null) {
            return;
        }
        resizeChain(Minecraft.getInstance());
    }

    static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RailgunState state = RailgunState.getInstance();
        boolean strikeActive = state.isStrikeActive() && state.getStrikeDimension() != null && state.getStrikeDimension().equals(minecraft.level.dimension());
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
        }

        boolean shaderpackActive = OculusCompat.isShaderpackActive();
        boolean compatActive = shouldUseCompat(shaderpackActive);
        boolean overlayActive = shouldDrawCompatOverlay(shaderpackActive, compatActive);
        if (compatActive != compatModeActive || overlayActive != compatOverlayEnabled) {
            compatModeActive = compatActive;
            compatOverlayEnabled = overlayActive;

            if (compatModeActive) {
                closeChain();
                if (compatOverlayEnabled && compatOverlayEffect == null) {
                    loadCompatOverlay(minecraft.getResourceManager());
                }
            } else {
                closeCompatOverlay();
                if (!chainReady || railgunChain == null) {
                    loadChain(minecraft, minecraft.getResourceManager());
                }
            }

            if (!compatOverlayEnabled) {
                closeCompatOverlay();
            }

            logShaderpackState("state-change", shaderpackActive, compatActive, overlayActive);
        }

        if (compatActive) {
            return;
        }

        if (!chainReady || railgunChain == null) {
            if (!compatModeActive) {
                loadChain(minecraft, minecraft.getResourceManager());
            }
            if (!chainReady || railgunChain == null) {
                return;
            }
        }

        resizeChain(minecraft);

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

        applyUniforms(modelView, projection, inverseProjection, cameraPos, targetPos, distance, timeSeconds, isBlockHit, strikeActive, state);

        railgunChain.process(event.getPartialTick());
    }

    static void onGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        boolean shaderpackActive = OculusCompat.isShaderpackActive();
        boolean compatActive = shouldUseCompat(shaderpackActive);
        boolean drawOverlay = shouldDrawCompatOverlay(shaderpackActive, compatActive);
        if (!drawOverlay) {
            return;
        }

        if (compatOverlayEffect == null) {
            loadCompatOverlay(minecraft.getResourceManager());
            if (compatOverlayEffect == null) {
                return;
            }
        }

        RailgunState state = RailgunState.getInstance();
        boolean strikeActive = state.isStrikeActive() && state.getStrikeDimension() != null
                && minecraft.level != null && state.getStrikeDimension().equals(minecraft.level.dimension());
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
        }

        drawCompatOverlay(minecraft, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            OculusCompat.tick();
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RailgunState state = RailgunState.getInstance();
        state.tick(minecraft);

        LocalPlayer player = minecraft.player;
        boolean attackDown = player != null && minecraft.options != null && minecraft.options.keyAttack.isDown();
        if (attackDown && !attackWasDown && state.canRequestFire(player)) {
            attemptFire(minecraft, state, player);
        }
        attackWasDown = attackDown;

        handleWorldLogging(minecraft);
    }

    static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (RailgunState.getInstance().isCharging()) {
            double baseFov = Minecraft.getInstance().options.fov().get();
            event.setFOV(baseFov);
        }
    }

    private static void drawCompatOverlay(Minecraft minecraft, int width, int height) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        boolean drewWithShader = false;
        VertexFormat formatUsed;
        if (compatOverlayEffect != null) {
            Uniform outSizeUniform = safeGetUniform(compatOverlayEffect, "OutSize");
            if (outSizeUniform != null) {
                outSizeUniform.set((float) width, (float) height);
            }
            Uniform timeUniform = safeGetUniform(compatOverlayEffect, "uTime");
            if (timeUniform != null) {
                float elapsedSeconds = (System.currentTimeMillis() - compatOverlayStartMs) / 1000.0F;
                timeUniform.set(elapsedSeconds);
            }
            Uniform intensityUniform = safeGetUniform(compatOverlayEffect, "uIntensity");
            if (intensityUniform != null) {
                intensityUniform.set(2.0F);
            }
            compatOverlayEffect.apply();
            formatUsed = DefaultVertexFormat.POSITION_TEX;
            bufferBuilder.begin(VertexFormat.Mode.QUADS, formatUsed);
            drewWithShader = true;
        } else if (hasCompatVignette) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, COMPAT_VIGNETTE_TEX);
            formatUsed = DefaultVertexFormat.POSITION_TEX;
            bufferBuilder.begin(VertexFormat.Mode.QUADS, formatUsed);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            formatUsed = DefaultVertexFormat.POSITION_COLOR;
            bufferBuilder.begin(VertexFormat.Mode.QUADS, formatUsed);
        }

        if (formatUsed == DefaultVertexFormat.POSITION_COLOR) {
            bufferBuilder.vertex(0.0D, height, 0.0D).color(0, 0, 0, 180).endVertex();
            bufferBuilder.vertex(0.0D, 0.0D, 0.0D).color(0, 0, 0, 180).endVertex();
            bufferBuilder.vertex(width, 0.0D, 0.0D).color(0, 0, 0, 180).endVertex();
            bufferBuilder.vertex(width, height, 0.0D).color(0, 0, 0, 180).endVertex();
        } else {
            bufferBuilder.vertex(0.0D, height, 0.0D).uv(0.0F, 1.0F).endVertex();
            bufferBuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
            bufferBuilder.vertex(width, 0.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
            bufferBuilder.vertex(width, height, 0.0D).uv(1.0F, 1.0F).endVertex();
        }

        tesselator.end();

        if (drewWithShader && compatOverlayEffect != null) {
            compatOverlayEffect.clear();
        }

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void handleWorldLogging(Minecraft minecraft) {
        if (!ClientConfig.COMPAT_LOG_IRIS_STATE.get()) {
            return;
        }
        Level level = minecraft.level;
        if (level == null) {
            lastLoggedWorld = null;
            return;
        }
        ResourceKey<Level> worldKey = level.dimension();
        if (worldKey.equals(lastLoggedWorld)) {
            return;
        }
        boolean shaderpackActive = OculusCompat.isShaderpackActive();
        boolean compatActive = shouldUseCompat(shaderpackActive);
        boolean overlayActive = shouldDrawCompatOverlay(shaderpackActive, compatActive);
        ForgeOrbitalRailgunMod.LOGGER.info("[orbital_railgun] Shaderpack active: {} | compat mode: {} | GUI overlay: {} (world: {})",
                shaderpackActive, compatActive, overlayActive, worldKey.location());
        lastLoggedWorld = worldKey;
    }

    private static void attemptFire(Minecraft minecraft, RailgunState state, LocalPlayer player) {
        if (minecraft.gameMode == null) {
            return;
        }
        HitResult hitResult = state.getCurrentHit();
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }
        BlockPos target = blockHitResult.getBlockPos();
        OrbitalRailgunItem item = state.getActiveRailgunItem();
        if (item == null) {
            return;
        }

        item.applyCooldown(player);
        minecraft.gameMode.releaseUsingItem(player);
        state.markFired();
        Network.CHANNEL.sendToServer(new C2S_RequestFire(target));
    }

    private static void applyUniforms(Matrix4f modelView, Matrix4f projection, Matrix4f inverseProjection, Vec3 cameraPos, Vec3 targetPos,
                                      float distance, float timeSeconds, float isBlockHit, boolean strikeActive, RailgunState state) {
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
            setFloat(effect, "StrikeActive", strikeActive ? 1.0F : 0.0F);
            setFloat(effect, "SelectionActive", state.isCharging() ? 1.0F : 0.0F);
            setInt(effect, "HitKind", state.getHitKind().ordinal());
        }
    }

    private static ResourceLocation getPassName(PostPass pass) {
        String name = pass.getName();
        return name != null ? ResourceLocation.tryParse(name) : null;
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
            ForgeOrbitalRailgunMod.LOGGER.error(
                    "Orbital railgun post chain passes had unexpected type: {}",
                    value == null ? "null" : value.getClass().getName()
            );
        } catch (IllegalAccessException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to access orbital railgun post chain passes", exception);
            return Collections.emptyList();
        }
        return Collections.emptyList();
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

    private static Uniform safeGetUniform(EffectInstance effect, String name) {
        try {
            return effect.getUniform(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = safeGetUniform(effect, name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private static void setVec3(EffectInstance effect, String name, Vec3 vec) {
        Uniform uniform = safeGetUniform(effect, name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }

    private static void setVec2(EffectInstance effect, String name, float x, float y) {
        Uniform uniform = safeGetUniform(effect, name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = safeGetUniform(effect, name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setInt(EffectInstance effect, String name, int value) {
        Uniform uniform = safeGetUniform(effect, name);
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

    private static void closeCompatOverlay() {
        if (compatOverlayEffect != null) {
            compatOverlayEffect.close();
            compatOverlayEffect = null;
            compatOverlayStartMs = 0L;
        }
    }

    private static void loadChain(Minecraft minecraft, ResourceManager resourceManager) {
        closeChain();
        if (minecraft.getMainRenderTarget() == null) {
            chainReady = false;
            return;
        }

        try {
            railgunChain = new PostChain(minecraft.getTextureManager(), resourceManager, minecraft.getMainRenderTarget(), RAILGUN_CHAIN_ID);
            chainReady = true;
            chainWidth = -1;
            chainHeight = -1;
            resizeChain(minecraft);
        } catch (IOException | RuntimeException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun post chain", exception);
            chainReady = false;
            closeChain();
        }
    }

    private static void loadCompatOverlay(ResourceManager resourceManager) {
        closeCompatOverlay();
        ResourceLocation jsonPath = ForgeOrbitalRailgunMod.id("shaders/program/compat_overlay.json");
        ResourceLocation vertexPath = ForgeOrbitalRailgunMod.id("shaders/program/compat_overlay.vsh");
        ResourceLocation fragmentPath = ForgeOrbitalRailgunMod.id("shaders/program/compat_overlay.fsh");
        if (resourceManager.getResource(jsonPath).isEmpty()) {
            ForgeOrbitalRailgunMod.LOGGER.error("Compat overlay JSON missing at {}", jsonPath);
        }
        if (resourceManager.getResource(vertexPath).isEmpty()) {
            ForgeOrbitalRailgunMod.LOGGER.error("Compat overlay vertex shader missing at {}", vertexPath);
        }
        if (resourceManager.getResource(fragmentPath).isEmpty()) {
            ForgeOrbitalRailgunMod.LOGGER.error("Compat overlay fragment shader missing at {}", fragmentPath);
        }
        try {
            compatOverlayEffect = new EffectInstance(
                    resourceManager,
                    COMPAT_OVERLAY_PROGRAM.toString()
            );
            compatOverlayStartMs = System.currentTimeMillis();
            ForgeOrbitalRailgunMod.LOGGER.info("[orbital_railgun] Loaded compat overlay program id: {}", COMPAT_OVERLAY_PROGRAM);
        } catch (IOException | RuntimeException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun compat overlay shader", exception);
            compatOverlayEffect = null;
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

    private static boolean shouldUseCompat(boolean shaderpackActive) {
        if (!shaderpackActive) {
            return false;
        }
        if (!ClientConfig.COMPAT_DISABLE_WITH_SHADERPACK.get()) {
            return false;
        }
        return !ClientConfig.COMPAT_FORCE_VANILLA_POSTCHAIN.get();
    }

    private static boolean shouldDrawCompatOverlay(boolean shaderpackActive, boolean compatActive) {
        if (!shaderpackActive) {
            return false;
        }
        if (!ClientConfig.COMPAT_OVERLAY_WITH_SHADERPACK.get()) {
            return false;
        }
        return compatActive;
    }

    private static void logShaderpackState(String context, boolean shaderpackActive, boolean compatActive, boolean overlayActive) {
        if (!ClientConfig.COMPAT_LOG_IRIS_STATE.get()) {
            return;
        }
        ForgeOrbitalRailgunMod.LOGGER.info("[orbital_railgun] Shaderpack active: {} | compat mode: {} | GUI overlay: {} ({})",
                shaderpackActive, compatActive, overlayActive, context);
    }

    @Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEventHandlers {
        private ForgeEventHandlers() {}

        @SubscribeEvent
        public static void onScreenRender(ScreenEvent.Render.Post event) {
            ClientEvents.onScreenRender(event);
        }

        @SubscribeEvent
        public static void onRenderStage(RenderLevelStageEvent event) {
            ClientEvents.onRenderStage(event);
        }

        @SubscribeEvent
        public static void onGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
            ClientEvents.onGuiOverlayPost(event);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            ClientEvents.onClientTick(event);
        }

        @SubscribeEvent
        public static void onComputeFov(ViewportEvent.ComputeFov event) {
            ClientEvents.onComputeFov(event);
        }
    }
}

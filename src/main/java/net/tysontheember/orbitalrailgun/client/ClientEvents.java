package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.fx.IrisCompat;
import net.tysontheember.orbitalrailgun.client.fx.RailgunFxRenderer;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.item.OrbitalRailgunItem;
import net.tysontheember.orbitalrailgun.network.C2S_RequestFire;
import net.tysontheember.orbitalrailgun.network.Network;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static final ResourceLocation RAILGUN_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/railgun.json");
    private static final Field PASSES_FIELD = findPassesField();
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
            ForgeOrbitalRailgunMod.id("strike"),
            ForgeOrbitalRailgunMod.id("gui")
    );

    private static PostChain railgunChain;
    private static boolean chainReady;
    private static int chainWidth = -1;
    private static int chainHeight = -1;

    private static boolean attackWasDown;

    static {
        if (PASSES_FIELD != null) {
            PASSES_FIELD.setAccessible(true);
        } else {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to locate orbital railgun post chain passes field");
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientEvents::onRegisterReloadListeners);
    }

    private ClientEvents() {}

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                ClientEvents.reloadChain(resourceManager);
            }
        });    }

    private static void reloadChain(ResourceManager resourceManager) {
        Minecraft minecraft = Minecraft.getInstance();
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
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun post chain", exception);
            chainReady = false;
            closeChain();
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

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!chainReady || railgunChain == null) {
            return;
        }
        resizeChain(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (!chainReady || railgunChain == null) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RailgunState state = RailgunState.getInstance();
        Level level = minecraft.level;
        boolean strikeActive = state.isStrikeActive() && state.getStrikeDimension() != null && state.getStrikeDimension().equals(level.dimension());
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
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

        if (IrisCompat.isShaderpackActive()) {
            RailgunFxRenderer.renderBeams(event, state);
            RailgunFxRenderer.renderScreenFx(event, state, event.getPartialTick());
        } else {
            applyUniforms(modelView, projection, inverseProjection, cameraPos, targetPos, distance, timeSeconds, isBlockHit, strikeActive, state);
            railgunChain.process(event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
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

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (RailgunState.getInstance().isCharging()) {
            double baseFov = Minecraft.getInstance().options.fov().get();
            event.setFOV(baseFov);
        }
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
}
package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static final ResourceLocation RAILGUN_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/railgun.json");
    private static final Set<ResourceLocation> MODEL_VIEW_UNIFORM_PASSES = Set.of(
            ForgeOrbitalRailgunMod.id("strike"),
            ForgeOrbitalRailgunMod.id("gui")
    );

    private static PostChain railgunChain;
    private static boolean chainReady;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    private static List<PassBinding> trackedPasses = Collections.emptyList();

    private static boolean attackWasDown;

    static {
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
            trackedPasses = PostChainPassTracker.capture(railgunChain);
            if (trackedPasses.isEmpty()) {
                handleMissingPasses();
                return;
            }
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

        applyUniforms(modelView, projection, inverseProjection, cameraPos, targetPos, distance, timeSeconds, isBlockHit, strikeActive, state);

        railgunChain.process(event.getPartialTick());
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
        if (trackedPasses.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget == null) {
            return;
        }

        float width = renderTarget.width > 0 ? renderTarget.width : renderTarget.viewWidth;
        float height = renderTarget.height > 0 ? renderTarget.height : renderTarget.viewHeight;

        for (PassBinding binding : trackedPasses) {
            EffectInstance effect = binding.effect();
            ResourceLocation passName = binding.name();
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

    private static void handleMissingPasses() {
        boolean shaderCompatPresent = isShaderCompatModPresent();
        if (shaderCompatPresent) {
            ForgeOrbitalRailgunMod.LOGGER.warn(
                    "Detected Iris/Oculus shader mod but could not locate orbital railgun post chain passes. Disabling the orbital railgun strike post-processing effect."
            );
        } else {
            ForgeOrbitalRailgunMod.LOGGER.warn(
                    "Could not locate orbital railgun post chain passes. Disabling the orbital railgun strike post-processing effect."
            );
        }
        closeChain();
    }

    private static boolean isShaderCompatModPresent() {
        ModList modList = ModList.get();
        return modList.isLoaded("oculus") || modList.isLoaded("iris");
    }

    private static final class PostChainPassTracker {
        private PostChainPassTracker() {
        }

        private static List<PassBinding> capture(PostChain chain) {
            if (chain == null) {
                return Collections.emptyList();
            }
            List<PostPass> passes = locatePasses(chain);
            if (passes.isEmpty()) {
                return Collections.emptyList();
            }

            List<PassBinding> bindings = new ArrayList<>(passes.size());
            for (PostPass pass : passes) {
                if (pass == null) {
                    continue;
                }
                EffectInstance effect = pass.getEffect();
                if (effect == null) {
                    continue;
                }
                String passName = pass.getName();
                ResourceLocation location = passName != null ? ResourceLocation.tryParse(passName) : null;
                bindings.add(new PassBinding(location, effect));
            }
            return bindings.isEmpty() ? Collections.emptyList() : List.copyOf(bindings);
        }

        private static List<PostPass> locatePasses(PostChain chain) {
            Class<?> type = chain.getClass();
            while (type != null) {
                Field[] fields = type.getDeclaredFields();
                for (Field field : fields) {
                    if (!List.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object value = field.get(chain);
                        if (!(value instanceof List<?> list)) {
                            continue;
                        }
                        List<PostPass> passes = extractPasses(list);
                        if (!passes.isEmpty()) {
                            return passes;
                        }
                    } catch (IllegalAccessException ignored) {
                    }
                }
                type = type.getSuperclass();
            }
            return Collections.emptyList();
        }

        private static List<PostPass> extractPasses(List<?> list) {
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            List<PostPass> passes = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof PostPass pass)) {
                    return Collections.emptyList();
                }
                passes.add(pass);
            }
            return passes;
        }
    }

    private record PassBinding(ResourceLocation name, EffectInstance effect) {
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
        trackedPasses = Collections.emptyList();
    }
}

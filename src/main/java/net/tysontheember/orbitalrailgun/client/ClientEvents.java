package net.tysontheember.orbitalrailgun.client;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.railgun.PostChainManager;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.item.OrbitalRailgunItem;
import net.tysontheember.orbitalrailgun.network.C2S_RequestFire;
import net.tysontheember.orbitalrailgun.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static boolean attackWasDown;

    static {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientEvents::onRegisterReloadListeners);
    }

    private ClientEvents() {}

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        PostChainManager.registerReloadListener(event);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        PostChainManager.onScreenResize(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (!PostChainManager.shouldHandleStage(event.getStage())) {
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

        PostChainManager.prepareFrame(minecraft);

        float timeSeconds = strikeActive
            ? state.getStrikeSeconds(event.getPartialTick())
            : state.getChargeSeconds(event.getPartialTick());

        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4f inverseProjection = new Matrix4f(projection).invert();
        Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose());
        Vec3 cameraPos = event.getCamera().getPosition();

        Vec3 strikePos = state.getStrikePos();
        Vec3 hitPos = state.getHitPos();
        Vec3 targetPos = strikeActive ? strikePos : hitPos;
        if (targetPos == null) {
            targetPos = cameraPos;
        }

        float distance = strikeActive && strikePos != null
            ? (float) cameraPos.distanceTo(strikePos)
            : state.getHitDistance();
        float isBlockHit = state.getHitKind() != RailgunState.HitKind.NONE ? 1.0F : 0.0F;

        PostChainManager.render(event, modelView, projection, inverseProjection, cameraPos, targetPos, distance,
            timeSeconds, isBlockHit, strikeActive, state);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PostChainManager.tick(minecraft);

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
        if (state.getStrikePos() == null) {
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
}

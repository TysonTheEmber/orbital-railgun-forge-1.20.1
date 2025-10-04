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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static boolean attackWasDown;

    private ClientEvents() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        PostChainManager.resizeIfNeeded();
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        PostChainManager.ensureState();
        if (!PostChainManager.isStageValid(event.getStage())) {
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

        PostChainManager.render(event, state, strikeActive, state.isCharging(), timeSeconds, projection, inverseProjection,
                modelView, cameraPos, targetPos, distance, isBlockHit);
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
}
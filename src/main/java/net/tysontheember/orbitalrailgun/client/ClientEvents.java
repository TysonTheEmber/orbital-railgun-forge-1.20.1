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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static boolean attackWasDown;

    static {
        PostChainManager.init();
    }

    private ClientEvents() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        PostChainManager.onResize();
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        PostChainManager.render(event, RailgunState.getInstance());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RailgunState state = RailgunState.getInstance();
        state.tick(minecraft);
        PostChainManager.tick(minecraft);

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

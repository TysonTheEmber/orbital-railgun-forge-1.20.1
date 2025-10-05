package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.compat.IrisDetector;
import net.tysontheember.orbitalrailgun.client.compat.ShaderPackAddonDetector;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.config.ClientConfig;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RenderHooks {
    private static boolean shaderPackLogged;
    private static boolean addonPresentLogged;
    private static boolean addonMissingLogged;

    private RenderHooks() {
    }

    @SubscribeEvent
    public static void onLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        RailgunState state = RailgunState.getInstance();
        boolean active = state.isStrikeActive() || state.isCharging();
        if (!active) {
            resetLogsIfNeeded(false);
            return;
        }

        boolean shaderPackEnabled = IrisDetector.isShaderPackEnabled();
        if (shaderPackEnabled && !ClientConfig.COMPAT_FORCE_VANILLA_POSTCHAIN.get()) {
            logShaderPackState();

            Minecraft minecraft = Minecraft.getInstance();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
            PoseStack poseStack = event.getPoseStack();
            Vec3 cameraPos = event.getCamera().getPosition();

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            OrbitalStrikeRenderer.renderBeamAndMarker(poseStack, bufferSource, state, event.getPartialTick(), cameraPos);
            bufferSource.endBatch();
            poseStack.popPose();
        } else {
            resetLogsIfNeeded(shaderPackEnabled);
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiOverlayEvent.Post event) {
        RailgunState state = RailgunState.getInstance();
        if (!state.isStrikeActive() && !state.isCharging()) {
            return;
        }
        boolean shaderPackEnabled = IrisDetector.isShaderPackEnabled();
        if (shaderPackEnabled && ClientConfig.COMPAT_OVERLAY_ENABLED.get()) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            HudOverlay.draw(guiGraphics, state);
        }
    }

    private static void logShaderPackState() {
        if (!shaderPackLogged) {
            ForgeOrbitalRailgunMod.LOGGER.info("[orbital_railgun] Shader-pack mode active: vanilla PostChain disabled; using geometry + add-on mask/composite.");
            shaderPackLogged = true;
        }
        boolean addonActive = ShaderPackAddonDetector.isAddonActive();
        if (addonActive) {
            if (!addonPresentLogged) {
                ForgeOrbitalRailgunMod.LOGGER.info("[orbital_railgun] OrbitalRailgun-Addon shader pack detected.");
                addonPresentLogged = true;
            }
            addonMissingLogged = false;
        } else if (!addonMissingLogged) {
            ForgeOrbitalRailgunMod.LOGGER.warn("[orbital_railgun] Add-on shader pack not detected; running geometry-only fallback (no composite warp).");
            addonMissingLogged = true;
            addonPresentLogged = false;
        }
    }

    private static void resetLogsIfNeeded(boolean shaderPackStillEnabled) {
        if (!shaderPackStillEnabled) {
            shaderPackLogged = false;
            addonPresentLogged = false;
            addonMissingLogged = false;
        }
    }
}

package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;

public final class HudOverlay {
    private HudOverlay() {
    }

    public static void draw(GuiGraphics guiGraphics, RailgunState state) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        float intensity = state.isStrikeActive()
                ? Mth.clamp(state.getStrikeSeconds(0.0F) / 6.0F, 0.0F, 1.0F)
                : (state.isCharging() ? Mth.clamp(state.getChargeSeconds(0.0F) / 3.0F, 0.0F, 1.0F) : 0.0F);

        if (intensity <= 0.0F) {
            return;
        }

        int alpha = (int) (180.0F * intensity);
        int color = (alpha << 24) | 0x00FFFFFF;

        RenderSystem.enableBlend();
        guiGraphics.fill(0, 0, width, height, color);
    }
}

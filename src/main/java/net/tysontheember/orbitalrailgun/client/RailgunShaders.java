package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class RailgunShaders {
    private static final ResourceLocation MARKER_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/strike_marker.json");

    private static PostChain markerChain;
    private static int markerWidth = -1;
    private static int markerHeight = -1;

    private RailgunShaders() {}

    public static void reload(Minecraft minecraft, ResourceManager resourceManager) {
        close();
        if (minecraft == null) return;

        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null) return;

        try {
            markerChain = new PostChain(
                    minecraft.getTextureManager(),
                    resourceManager,
                    target,
                    MARKER_CHAIN_ID
            );
            markerWidth = -1;
            markerHeight = -1;
            resize(minecraft);
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun marker post chain", exception);
            close();
        }
    }

    public static void resize(Minecraft minecraft) {
        if (markerChain == null || minecraft == null) return;

        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null) return;

        int width = target.width;
        int height = target.height;

        if (width == markerWidth && height == markerHeight) {
            return;
        }

        markerChain.resize(width, height);
        markerWidth = width;
        markerHeight = height;
    }

    public static void close() {
        if (markerChain != null) {
            try {
                markerChain.close();
            } catch (Exception ignored) {
            }
            markerChain = null;
        }
        markerWidth = -1;
        markerHeight = -1;
    }

    @Nullable
    public static PostChain marker() {
        return markerChain;
    }
}

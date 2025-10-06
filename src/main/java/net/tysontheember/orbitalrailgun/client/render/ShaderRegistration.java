package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.io.IOException;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ForgeOrbitalRailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ShaderRegistration {

    public static ShaderInstance ORB_BEAM_SHADER;
    public static ShaderInstance ORB_MARKER_SHADER;

    private ShaderRegistration() {}

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            // Looks for: assets/orbital_railgun/shaders/core/orb_beam.json
            ORB_BEAM_SHADER = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_beam"),
                    DefaultVertexFormat.POSITION_COLOR_TEX
            );
            event.registerShader(ORB_BEAM_SHADER, s -> {});

            // Looks for: assets/orbital_railgun/shaders/core/orb_marker.json
            ORB_MARKER_SHADER = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_marker"),
                    DefaultVertexFormat.POSITION_COLOR_TEX
            );
            event.registerShader(ORB_MARKER_SHADER, s -> {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to register Orbital Railgun shaders", e);
        }
    }
}

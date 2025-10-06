package net.tysontheember.orbitalrailgun.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat; // <-- correct import
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ShaderRegistration {
    public static ShaderInstance ORB_BEAM_SHADER;
    public static ShaderInstance ORB_MARKER_SHADER;

    private ShaderRegistration() {}

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ORB_BEAM_SHADER = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_beam"),   // no "core/"
                    DefaultVertexFormat.POSITION_COLOR_TEX
            );
            event.registerShader(ORB_BEAM_SHADER, s -> ORB_BEAM_SHADER = s);

            ORB_MARKER_SHADER = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_marker"), // no "core/"
                    DefaultVertexFormat.POSITION_COLOR_TEX
            );
            event.registerShader(ORB_MARKER_SHADER, s -> ORB_MARKER_SHADER = s);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register Orbital Railgun shaders", e);
        }
    }
}

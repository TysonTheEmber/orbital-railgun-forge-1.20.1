package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ShaderRegistration {

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ShaderInstance beam = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation("orbital_railgun", "orb_beam"),
                    DefaultVertexFormat.POSITION_COLOR_TEX
            );
            event.registerShader(beam, shader -> ClientRender.BEAM_SHADER = shader);

            ShaderInstance marker = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation("orbital_railgun", "orb_marker"),
                    DefaultVertexFormat.POSITION_COLOR_TEX
            );
            event.registerShader(marker, shader -> ClientRender.MARKER_SHADER = shader);

        } catch (Exception e) {
            throw new RuntimeException("Failed to register Orbital Railgun shaders", e);
        }
    }
}

package net.tysontheember.orbitalrailgun.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ShaderRegistration {
    private ShaderRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        ShaderInstance beam = new ShaderInstance(
                event.getResourceProvider(),
                new ResourceLocation("orbital_railgun", "core/orb_beam"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
        event.registerShader(beam, shader -> ClientRender.ORB_BEAM_SHADER = shader);

        ShaderInstance marker = new ShaderInstance(
                event.getResourceProvider(),
                new ResourceLocation("orbital_railgun", "core/orb_marker"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
        event.registerShader(marker, shader -> ClientRender.ORB_MARKER_SHADER = shader);
    }
}

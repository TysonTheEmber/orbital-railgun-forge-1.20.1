package net.tysontheember.orbitalrailgun.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.fx.RailgunFxRenderer;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Intentionally empty. The Fabric version did not use custom keybinds.
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        var resourceProvider = event.getResourceProvider();
        event.registerShader(
            new ShaderInstance(resourceProvider, new ResourceLocation("orbital_railgun", "orbital_screen_distort"), DefaultVertexFormat.POSITION),
            shader -> RailgunFxRenderer.SCREEN_DISTORT = shader
        );
        event.registerShader(
            new ShaderInstance(resourceProvider, new ResourceLocation("orbital_railgun", "orbital_screen_tint"), DefaultVertexFormat.POSITION),
            shader -> RailgunFxRenderer.SCREEN_TINT = shader
        );
        event.registerShader(
            new ShaderInstance(resourceProvider, new ResourceLocation("orbital_railgun", "orbital_beam"), DefaultVertexFormat.POSITION),
            shader -> RailgunFxRenderer.BEAM = shader
        );
    }
}

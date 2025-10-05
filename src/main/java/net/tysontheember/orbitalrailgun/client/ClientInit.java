package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.fx.RailgunFxRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
        event.registerShader(
            new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(), ForgeOrbitalRailgunMod.id("orbital_screen_distort"), DefaultVertexFormat.POSITION),
            RailgunFxRenderer::setScreenDistortShader
        );
        event.registerShader(
            new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(), ForgeOrbitalRailgunMod.id("orbital_screen_tint"), DefaultVertexFormat.POSITION),
            RailgunFxRenderer::setScreenTintShader
        );
        event.registerShader(
            new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(), ForgeOrbitalRailgunMod.id("orbital_beam"), DefaultVertexFormat.POSITION_COLOR_TEX),
            RailgunFxRenderer::setBeamShader
        );
    }
}

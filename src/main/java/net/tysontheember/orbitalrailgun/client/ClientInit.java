package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
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
        ShaderInstance distort = new ShaderInstance(event.getResourceProvider(), new ResourceLocation("orbital_railgun", "orbital_screen_distort"), DefaultVertexFormat.POSITION);
        event.registerShader(distort, shader -> RailgunFxRenderer.SCREEN_DISTORT = shader);

        ShaderInstance tint = new ShaderInstance(event.getResourceProvider(), new ResourceLocation("orbital_railgun", "orbital_screen_tint"), DefaultVertexFormat.POSITION);
        event.registerShader(tint, shader -> RailgunFxRenderer.SCREEN_TINT = shader);

        ShaderInstance beam = new ShaderInstance(event.getResourceProvider(), new ResourceLocation("orbital_railgun", "orbital_beam"), DefaultVertexFormat.POSITION);
        event.registerShader(beam, shader -> RailgunFxRenderer.BEAM = shader);
    }
}

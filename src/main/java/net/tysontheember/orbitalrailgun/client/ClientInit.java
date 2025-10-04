package net.tysontheember.orbitalrailgun.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
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
    public static ShaderInstance ORB_FS;

    private ClientInit() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Intentionally empty. The Fabric version did not use custom keybinds.
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();
        event.registerShader(
                new ShaderInstance(provider, new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_fs"), DefaultVertexFormat.POSITION),
                shader -> ORB_FS = shader
        );
    }
}

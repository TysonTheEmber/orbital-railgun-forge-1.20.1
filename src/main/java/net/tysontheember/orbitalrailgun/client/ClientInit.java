package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

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
    public static void registerShaders(RegisterShadersEvent e) throws IOException {
        ResourceProvider p = e.getResourceProvider();
        e.registerShader(
            new ShaderInstance(p, ForgeOrbitalRailgunMod.id("orb_fs"), DefaultVertexFormat.POSITION),
            s -> ORB_FS = s
        );
    }
}

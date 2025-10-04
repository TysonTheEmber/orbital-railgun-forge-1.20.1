package net.tysontheember.orbitalrailgun.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientInit {
    public static ShaderInstance ORB_FS;

    private ClientInit() {}

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent e) throws IOException {
        ResourceProvider p = e.getResourceProvider();
        e.registerShader(
            new ShaderInstance(p, new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_fs"), DefaultVertexFormat.POSITION),
            s -> ORB_FS = s
        );
    }
}

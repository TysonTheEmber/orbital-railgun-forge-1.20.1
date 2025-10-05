package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
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
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();

        // Match the CompatDraw geometry (POSITION-only full-screen triangle)
        ResourceLocation progId = ResourceLocation.fromNamespaceAndPath(ForgeOrbitalRailgunMod.MOD_ID, "orb_fs");
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "orb_fs"),
                        DefaultVertexFormat.POSITION_TEX // matches ["Position","UV0"] in JSON
                ),
                shader -> ORB_FS = shader
        );
        // NOTE: Don't call setSampler() here — PostChain handles samplers in the JSON,
        // and CompatDraw binds textures explicitly before drawing.
    }
}

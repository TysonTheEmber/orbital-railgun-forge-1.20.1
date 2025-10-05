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
@Mod.EventBusSubscriber(
        modid = ForgeOrbitalRailgunMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientInit {
    // Needed by CompatDraw
    public static ShaderInstance ORB_FS;

    private ClientInit() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // No custom keybinds
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();

        // Register a core shader instance so CompatDraw can use it directly.
        // Use POSITION because the fullscreen tri only provides positions.
        event.registerShader(
                new ShaderInstance(provider, ForgeOrbitalRailgunMod.id("orb_fs"), DefaultVertexFormat.POSITION),
                shader -> ORB_FS = shader
        );

        // Do NOT bind samplers here; CompatDraw sets them at draw time.
    }
}

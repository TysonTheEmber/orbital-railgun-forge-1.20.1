package net.tysontheember.orbitalrailgun.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public final class ClientRender {
    public static ShaderInstance BEAM_SHADER;
    public static ShaderInstance MARKER_SHADER;

    // 1x1 white mask texture (exists under assets/orbital_railgun/textures/mask/mask.png)
    public static final ResourceLocation MASK_TEX = new ResourceLocation("orbital_railgun", "textures/mask/mask.png");

    private ClientRender() {}
}

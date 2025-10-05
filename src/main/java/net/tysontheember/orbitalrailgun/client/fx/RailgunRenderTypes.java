package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

public final class RailgunRenderTypes {
    public static final RenderType SCREEN_FX_DISTORT = RenderType.create(
        "orbital_screen_fx_distort",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderType.ShaderStateShard(() -> GameRenderer.getShader(ForgeOrbitalRailgunMod.id("orbital_screen_distort"))))
            .setTransparencyState(RenderType.ADDITIVE_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setDepthTestState(RenderType.NO_DEPTH_TEST)
            .createCompositeState(false)
    );

    public static final RenderType SCREEN_FX_TINT = RenderType.create(
        "orbital_screen_fx_tint",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderType.ShaderStateShard(() -> GameRenderer.getShader(ForgeOrbitalRailgunMod.id("orbital_screen_tint"))))
            .setTransparencyState(RenderType.ADDITIVE_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setDepthTestState(RenderType.NO_DEPTH_TEST)
            .createCompositeState(false)
    );

    public static final RenderType BEAM_ADDITIVE = RenderType.create(
        "orbital_beam_additive",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLE_STRIP,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderType.ShaderStateShard(() -> GameRenderer.getShader(ForgeOrbitalRailgunMod.id("orbital_beam"))))
            .setTransparencyState(RenderType.ADDITIVE_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
            .createCompositeState(false)
    );

    private RailgunRenderTypes() {
    }
}

package net.tysontheember.orbitalrailgun.client.fx;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;

public final class RailgunRenderTypes {
    private RailgunRenderTypes() {
    }

    public static final RenderType SCREEN_FX_DISTORT = RenderType.create(
        "orbital_screen_fx_distort",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> GameRenderer.getShader("orbital_railgun:orbital_screen_distort")))
            .setTransparencyState(RenderType.ADDITIVE_TRANSPARENCY)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .setOutputState(RenderType.MAIN_TARGET)
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
            .setShaderState(new ShaderStateShard(() -> GameRenderer.getShader("orbital_railgun:orbital_screen_tint")))
            .setTransparencyState(RenderType.ADDITIVE_TRANSPARENCY)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .setOutputState(RenderType.MAIN_TARGET)
            .createCompositeState(false)
    );

    public static final RenderType BEAM_ADDITIVE = RenderType.create(
        "orbital_beam_additive",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> GameRenderer.getShader("orbital_railgun:orbital_beam")))
            .setTransparencyState(RenderType.ADDITIVE_TRANSPARENCY)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .setOutputState(RenderType.MAIN_TARGET)
            .createCompositeState(false)
    );
}

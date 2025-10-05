package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.function.Supplier;

public final class RailgunRenderTypes extends RenderType {
    private static final Supplier<ShaderInstance> SCREEN_DISTORT_SUP = () -> RailgunFxRenderer.SCREEN_DISTORT;
    private static final Supplier<ShaderInstance> SCREEN_TINT_SUP = () -> RailgunFxRenderer.SCREEN_TINT;
    private static final Supplier<ShaderInstance> BEAM_SUP = () -> RailgunFxRenderer.BEAM;

    public static final RenderType SCREEN_FX_DISTORT = RenderType.create(
        "orbital_screen_fx_distort",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(SCREEN_DISTORT_SUP))
            .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
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
            .setShaderState(new RenderStateShard.ShaderStateShard(SCREEN_TINT_SUP))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .createCompositeState(false)
    );

    public static final RenderType BEAM_ADDITIVE = RenderType.create(
        "orbital_beam_additive",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(BEAM_SUP))
            .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );

    private RailgunRenderTypes(String name, VertexFormat vertexFormat, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, vertexFormat, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}

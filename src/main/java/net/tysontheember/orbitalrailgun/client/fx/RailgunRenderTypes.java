package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tysontheember.orbitalrailgun.client.fx.RailgunFxRenderer;

import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;

@OnlyIn(Dist.CLIENT)
public final class RailgunRenderTypes {
    private static final RenderStateShard.TransparencyStateShard ORBITAL_ADDITIVE = new RenderStateShard.TransparencyStateShard(
        "orbital_additive",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );

    private static final RenderStateShard.DepthTestStateShard ORBITAL_NO_DEPTH = new RenderStateShard.DepthTestStateShard("orbital_no_depth", GL_ALWAYS) {
        @Override
        public void setupRenderState() {
            RenderSystem.disableDepthTest();
        }

        @Override
        public void clearRenderState() {
            RenderSystem.enableDepthTest();
        }
    };

    private static final RenderStateShard.DepthTestStateShard ORBITAL_LEQUAL = new RenderStateShard.DepthTestStateShard("orbital_lequal", GL_LEQUAL);

    private static final RenderStateShard.CullStateShard ORBITAL_NO_CULL = new RenderStateShard.CullStateShard(false);
    private static final RenderStateShard.WriteMaskStateShard ORBITAL_COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);

    private static final RenderStateShard.ShaderStateShard SCREEN_DISTORT_SHADER = new RenderStateShard.ShaderStateShard(RailgunFxRenderer::getScreenDistortShader);
    private static final RenderStateShard.ShaderStateShard SCREEN_TINT_SHADER = new RenderStateShard.ShaderStateShard(RailgunFxRenderer::getScreenTintShader);
    private static final RenderStateShard.ShaderStateShard BEAM_SHADER = new RenderStateShard.ShaderStateShard(RailgunFxRenderer::getBeamShader);

    public static final RenderType SCREEN_FX_DISTORT = RenderType.create(
        "orbital_screen_fx_distort",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(SCREEN_DISTORT_SHADER)
            .setTransparencyState(ORBITAL_ADDITIVE)
            .setDepthTestState(ORBITAL_NO_DEPTH)
            .setWriteMaskState(ORBITAL_COLOR_WRITE)
            .createCompositeState(false)
    );

    public static final RenderType SCREEN_FX_TINT = RenderType.create(
        "orbital_screen_fx_tint",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(SCREEN_TINT_SHADER)
            .setTransparencyState(ORBITAL_ADDITIVE)
            .setDepthTestState(ORBITAL_NO_DEPTH)
            .setWriteMaskState(ORBITAL_COLOR_WRITE)
            .createCompositeState(false)
    );

    public static final RenderType BEAM_ADDITIVE = RenderType.create(
        "orbital_beam_additive",
        DefaultVertexFormat.POSITION_COLOR_TEX,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(BEAM_SHADER)
            .setTransparencyState(ORBITAL_ADDITIVE)
            .setCullState(ORBITAL_NO_CULL)
            .setWriteMaskState(ORBITAL_COLOR_WRITE)
            .setDepthTestState(ORBITAL_LEQUAL)
            .createCompositeState(false)
    );

    private RailgunRenderTypes() {
    }
}

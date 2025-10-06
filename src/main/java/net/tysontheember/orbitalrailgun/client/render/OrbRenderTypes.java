package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class OrbRenderTypes {

    // Custom public clones of the protected vanilla state shards
    private static final RenderStateShard.TransparencyStateShard ORB_TRANSLUCENT =
            new RenderStateShard.TransparencyStateShard(
                    "orb_translucent",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    private static final RenderStateShard.CullStateShard ORB_NO_CULL =
            new RenderStateShard.CullStateShard(false);

    // GL_LEQUAL == 0x0203
    private static final RenderStateShard.DepthTestStateShard ORB_LEQUAL_DEPTH =
            new RenderStateShard.DepthTestStateShard("orb_lequal", 0x0203);

    private static final RenderStateShard.WriteMaskStateShard ORB_COLOR_WRITE =
            new RenderStateShard.WriteMaskStateShard(true, false);

    private OrbRenderTypes() {}

    public static RenderType beam(ResourceLocation maskTex) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> ShaderRegistration.ORB_BEAM_SHADER))
                .setTextureState(new RenderStateShard.TextureStateShard(maskTex, false, false))
                .setTransparencyState(ORB_TRANSLUCENT)
                .setCullState(ORB_NO_CULL)
                .setDepthTestState(ORB_LEQUAL_DEPTH)
                .setWriteMaskState(ORB_COLOR_WRITE)
                .createCompositeState(true);

        return RenderType.create(
                "orbital_railgun:orb_beam",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,  // does not affect crumbling
                true,   // sort on upload (translucent)
                state
        );
    }

    public static RenderType marker(ResourceLocation maskTex) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> ShaderRegistration.ORB_MARKER_SHADER))
                .setTextureState(new RenderStateShard.TextureStateShard(maskTex, false, false))
                .setTransparencyState(ORB_TRANSLUCENT)
                .setCullState(ORB_NO_CULL)
                .setDepthTestState(ORB_LEQUAL_DEPTH)
                .setWriteMaskState(ORB_COLOR_WRITE)
                .createCompositeState(true);

        return RenderType.create(
                "orbital_railgun:orb_marker",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                state
        );
    }
}

package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class OrbRenderTypes {
    private static RenderType make(ResourceLocation tex, RenderStateShard.ShaderStateShard shader) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(shader)
                .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                        "orb_translucent",
                        () -> { RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); },
                        RenderSystem::disableBlend
                ))
                .setDepthTestState(new RenderStateShard.DepthTestStateShard("lequal", GL11.GL_LEQUAL))
                .setCullState(new RenderStateShard.CullStateShard(false))
                .createCompositeState(false);

        return RenderType.create(
                "orb_rt",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,      // buffer size
                false,    // affectsCrumbling
                true,     // sortOnUpload (good for translucency)
                state
        );
    }

    public static RenderType beam(ResourceLocation tex) {
        return make(tex, new RenderStateShard.ShaderStateShard(() -> ClientRender.ORB_BEAM_SHADER));
    }

    public static RenderType marker(ResourceLocation tex) {
        return make(tex, new RenderStateShard.ShaderStateShard(() -> ClientRender.ORB_MARKER_SHADER));
    }
}

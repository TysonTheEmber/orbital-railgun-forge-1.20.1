package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mojang.blaze3d.shaders.Uniform;

import java.io.IOException;

/**
 * Lightweight overlay drawn when an Iris/Oculus shader-pack is active. It keeps
 * the screen intact by sampling the already-rendered color buffer instead of
 * touching the vanilla post chain or clearing the framebuffer.
 */
public final class FallbackPostRenderer {
    private static final ResourceLocation SHADER_ID = ForgeOrbitalRailgunMod.id("fallback_post");

    private static ShaderInstance shader;

    private FallbackPostRenderer() {}

    public static boolean reload(ResourceManager resourceManager) {
        close();
        try {
            shader = new ShaderInstance(resourceManager, SHADER_ID, DefaultVertexFormat.POSITION_TEX);
            return true;
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun fallback shader", exception);
            shader = null;
            return false;
        }
    }

    public static void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }

    public static boolean isReady() {
        return shader != null;
    }

    public static void render(RenderTarget renderTarget) {
        if (shader == null || renderTarget == null) {
            return;
        }
        int width = renderTarget.width > 0 ? renderTarget.width : renderTarget.viewWidth;
        int height = renderTarget.height > 0 ? renderTarget.height : renderTarget.viewHeight;
        if (width <= 0 || height <= 0) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DiffuseSampler", renderTarget::getColorTextureId);
        Uniform outSize = shader.getUniform("OutSize");
        if (outSize != null) {
            outSize.set((float) width, (float) height);
        }
        RenderSystem.setShaderTexture(0, renderTarget.getColorTextureId());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.viewport(0, 0, width, height);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        builder.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        builder.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        builder.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}

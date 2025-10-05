package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;

public final class CompatDraw {
    private static VertexBuffer FS_TRI;

    private CompatDraw() {}

    private static void ensureGeom() {
        if (FS_TRI != null) return;

        FS_TRI = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        // Big full-screen triangle (no UVs needed; we use gl_FragCoord in the shader)
        builder.vertex(-1, -1, 0).endVertex();
        builder.vertex( 3, -1, 0).endVertex();
        builder.vertex(-1,  3, 0).endVertex();
        FS_TRI.bind();
        FS_TRI.upload(builder.end());
        VertexBuffer.unbind();
    }

    public static void render(float partialTicks, int hitKind, Vec3 hitPos) {
        ensureGeom();
        ShaderInstance shader = ClientInit.ORB_FS;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();
        if (mainTarget == null) return;

        // Bind the scene color to texture unit 0
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(mainTarget.getColorTextureId());
        shader.setSampler("SceneColor", mainTarget.getColorTextureId());

        // Bind depth if available to unit 1 (shader declares SceneDepth but will work fine if missing)
        int depthId = mainTarget.getDepthTextureId();
        if (depthId != -1) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(depthId);
            shader.setSampler("SceneDepth", depthId);
        }

        // Feed custom uniforms (all optional-safe in GLSL)
        var uni = shader.getUniform("iTime");
        if (uni != null && mc.level != null) {
            uni.set((mc.level.getGameTime() + partialTicks) / 20.0F);
        }
        uni = shader.getUniform("HitKind");
        if (uni != null) {
            uni.set(hitKind);
        }
        uni = shader.getUniform("HitPos");
        if (uni != null && hitPos != null) {
            uni.set((float) hitPos.x, (float) hitPos.y, (float) hitPos.z);
        }

        RenderSystem.disableDepthTest();
        shader.apply();

        FS_TRI.bind();
        DefaultVertexFormat.POSITION.setupBufferState();
        FS_TRI.drawWithShader(new Matrix4f().identity(), RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
        DefaultVertexFormat.POSITION.clearBufferState();

        shader.clear();
        RenderSystem.enableDepthTest();
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
    }
}

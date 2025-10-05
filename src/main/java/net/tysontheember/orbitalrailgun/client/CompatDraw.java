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
        if (FS_TRI != null) {
            return;
        }
        FS_TRI = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        builder.vertex(-1, -1, 0).endVertex();
        builder.vertex(3, -1, 0).endVertex();
        builder.vertex(-1, 3, 0).endVertex();
        FS_TRI.bind();
        FS_TRI.upload(builder.end());
        VertexBuffer.unbind();
    }

    public static void render(float partialTicks, int hitKind, Vec3 hitPos) {
        ensureGeom();

        ShaderInstance shader = ClientInit.ORB_FS;
        if (shader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }

        // Bind scene color to unit 0 and depth (if present) to unit 1
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(mainTarget.getColorTextureId());
        shader.setSampler("SceneColor", mainTarget.getColorTextureId());

        int depthId = mainTarget.getDepthTextureId();
        if (depthId != -1) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(depthId);
            shader.setSampler("SceneDepth", depthId);
        }

        // ---- Uniforms ----
        // Screen size (OutSize)
        float width = mainTarget.width > 0 ? mainTarget.width : mainTarget.viewWidth;
        float height = mainTarget.height > 0 ? mainTarget.height : mainTarget.viewHeight;
        var outSizeU = shader.getUniform("OutSize");
        if (outSizeU != null) outSizeU.set(width, height);

        // Inverse projection (InverseTransformMatrix)
        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f invProj = new Matrix4f(proj).invert();
        var invU = shader.getUniform("InverseTransformMatrix");
        if (invU != null) invU.set(invProj);

        // CameraPosition
        var cam = minecraft.gameRenderer.getMainCamera();
        if (cam != null) {
            var camU = shader.getUniform("CameraPosition");
            if (camU != null) {
                var cp = cam.getPosition();
                camU.set((float) cp.x, (float) cp.y, (float) cp.z);
            }
        }

        // iTime
        var timeU = shader.getUniform("iTime");
        if (timeU != null && minecraft.level != null) {
            timeU.set((minecraft.level.getGameTime() + partialTicks) / 20.0F);
        }

        // HitKind
        var hitKindU = shader.getUniform("HitKind");
        if (hitKindU != null) hitKindU.set(hitKind);

        // HitPos
        var hitPosU = shader.getUniform("HitPos");
        if (hitPosU != null && hitPos != null) {
            hitPosU.set((float) hitPos.x, (float) hitPos.y, (float) hitPos.z);
        }

        // Draw full-screen triangle
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

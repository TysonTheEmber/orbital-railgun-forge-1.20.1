package net.tysontheember.orbitalrailgun.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
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
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(3, -1, 0).endVertex();
        bb.vertex(-1, 3, 0).endVertex();
        FS_TRI.bind();
        FS_TRI.upload(bb.end());
        VertexBuffer.unbind();
    }

    public static void render(float pt, int hitKind, Vec3 hitPos) {
        ensureGeom();
        ShaderInstance fx = ClientInit.ORB_FS;
        if (fx == null) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null) return;

        RenderSystem.disableDepthTest();

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(main.getColorTextureId());
        fx.setSampler("SceneColor", main.getColorTextureId());

        int depthId = main.getDepthTextureId();
        if (depthId != -1) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(depthId);
            fx.setSampler("SceneDepth", depthId);
        } else {
            fx.setSampler("SceneDepth", 0);
        }

        setIfPresent(fx, "iTime", u -> u.set((mc.level != null ? (mc.level.getGameTime() + pt) : pt) / 20.0f));
        setIfPresent(fx, "HitKind", u -> u.set(hitKind));
        if (hitPos != null) {
            setIfPresent(fx, "HitPos", u -> u.set((float) hitPos.x, (float) hitPos.y, (float) hitPos.z));
        }

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        fx.apply();
        FS_TRI.bind();
        DefaultVertexFormat.POSITION.setupBufferState(0L);
        FS_TRI.drawWithShader(new Matrix4f().identity(), RenderSystem.getProjectionMatrix(), fx);
        VertexBuffer.unbind();
        fx.clear();

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.enableDepthTest();
    }

    private static void setIfPresent(ShaderInstance fx, String name, java.util.function.Consumer<com.mojang.blaze3d.shaders.Uniform> f) {
        var u = fx.getUniform(name);
        if (u != null) {
            f.accept(u);
        }
    }
}

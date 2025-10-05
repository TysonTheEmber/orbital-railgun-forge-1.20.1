package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class RailgunFxRenderer {
    private static ShaderInstance screenDistortShader;
    private static ShaderInstance screenTintShader;
    private static ShaderInstance beamShader;

    private static Uniform screenDistortTime;
    private static Uniform screenDistortFlash;
    private static Uniform screenDistortHitKind;
    private static Uniform screenDistortHitPos;
    private static Uniform screenDistortDistance;
    private static Uniform screenDistortScreenSize;
    private static Uniform screenDistortHasGrab;

    private static Uniform screenTintTime;
    private static Uniform screenTintFlash;

    private static Uniform beamTime;
    private static Uniform beamFlash;

    private RailgunFxRenderer() {
    }

    public static void setScreenDistortShader(ShaderInstance shader) {
        screenDistortShader = shader;
        screenDistortTime = shader.getUniform("Time");
        screenDistortFlash = shader.getUniform("Flash01");
        screenDistortHitKind = shader.getUniform("HitKind");
        screenDistortHitPos = shader.getUniform("HitPos");
        screenDistortDistance = shader.getUniform("Distance");
        screenDistortScreenSize = shader.getUniform("ScreenSize");
        screenDistortHasGrab = shader.getUniform("HasGrab");
    }

    public static void setScreenTintShader(ShaderInstance shader) {
        screenTintShader = shader;
        screenTintTime = shader.getUniform("Time");
        screenTintFlash = shader.getUniform("Flash01");
    }

    public static void setBeamShader(ShaderInstance shader) {
        beamShader = shader;
        beamTime = shader.getUniform("Time");
        beamFlash = shader.getUniform("Flash01");
    }

    public static ShaderInstance getScreenDistortShader() {
        return screenDistortShader;
    }

    public static ShaderInstance getScreenTintShader() {
        return screenTintShader;
    }

    public static ShaderInstance getBeamShader() {
        return beamShader;
    }

    public static void renderBeams(RenderLevelStageEvent event, RailgunState state) {
        if (screenDistortShader == null || beamShader == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        if (!state.isStrikeActive() && !state.isCharging()) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 targetPos = state.isStrikeActive() ? state.getStrikePos() : state.getHitPos();
        if (targetPos == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer beamConsumer = bufferSource.getBuffer(RailgunRenderTypes.BEAM_ADDITIVE);

        float timeSeconds = state.isStrikeActive()
            ? state.getStrikeSeconds(event.getPartialTick())
            : state.getChargeSeconds(event.getPartialTick());
        setUniform(beamTime, timeSeconds);
        setUniform(beamFlash, getFlashValue(state, event.getPartialTick()));

        renderBeamColumn(poseStack, beamConsumer, targetPos);
        renderShockwave(poseStack, beamConsumer, targetPos);

        poseStack.popPose();
        bufferSource.endBatch(RailgunRenderTypes.BEAM_ADDITIVE);
    }

    public static void renderScreenFx(RenderLevelStageEvent event, RailgunState state, float partialTick) {
        if (screenDistortShader == null || screenTintShader == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!state.isStrikeActive() && !state.isCharging()) {
            return;
        }

        float timeSeconds = state.isStrikeActive()
            ? state.getStrikeSeconds(partialTick)
            : state.getChargeSeconds(partialTick);
        float flashValue = getFlashValue(state, partialTick);

        Vec3 hitPos = state.isStrikeActive() ? state.getStrikePos() : state.getHitPos();
        float distance = state.isStrikeActive()
            ? (float) event.getCamera().getPosition().distanceTo(state.getStrikePos())
            : state.getHitDistance();
        int hitKind = state.getHitKind().ordinal();

        Window window = minecraft.getWindow();
        float screenWidth = (float) window.getWidth();
        float screenHeight = (float) window.getHeight();

        setUniform(screenDistortTime, timeSeconds);
        setUniform(screenDistortFlash, flashValue);
        setUniform(screenDistortHitKind, hitKind);
        if (hitPos != null) {
            setUniform(screenDistortHitPos, (float) hitPos.x, (float) hitPos.y, (float) hitPos.z);
        }
        setUniform(screenDistortDistance, distance);
        setUniform(screenDistortScreenSize, screenWidth, screenHeight);
        setUniform(screenDistortHasGrab, 0);

        setUniform(screenTintTime, timeSeconds);
        setUniform(screenTintFlash, flashValue);

        renderFullscreenTriangle(RailgunRenderTypes.SCREEN_FX_DISTORT);
        renderFullscreenTriangle(RailgunRenderTypes.SCREEN_FX_TINT);
    }

    private static void renderFullscreenTriangle(RenderType renderType) {
        if (renderType == RailgunRenderTypes.SCREEN_FX_DISTORT) {
            RenderSystem.setShader(() -> screenDistortShader);
        } else {
            RenderSystem.setShader(() -> screenTintShader);
        }
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        builder.vertex(-1.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(3.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(-1.0F, 3.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static float getFlashValue(RailgunState state, float partialTick) {
        if (state.isStrikeActive()) {
            float t = state.getStrikeSeconds(partialTick);
            return Mth.clamp(1.5F - t * 0.5F, 0.0F, 1.0F);
        }
        if (state.isCharging()) {
            float t = state.getChargeSeconds(partialTick);
            return Mth.clamp(t * 0.25F, 0.0F, 0.8F);
        }
        return 0.0F;
    }

    private static void renderBeamColumn(PoseStack poseStack, VertexConsumer consumer, Vec3 targetPos) {
        float beamRadius = 4.0F;
        float maxHeight = 256.0F;
        float minHeight = (float) targetPos.y - 4.0F;
        float topHeight = Math.min(maxHeight, (float) targetPos.y + 128.0F);

        Matrix4f pose = poseStack.last().pose();
        float x = (float) targetPos.x;
        float z = (float) targetPos.z;
        float yBottom = Math.max(-64.0F, minHeight);
        float yTop = Math.max(yBottom + 4.0F, topHeight);

        addQuad(consumer, pose, x - beamRadius, yBottom, z - beamRadius, x - beamRadius, yTop, z - beamRadius, x + beamRadius, yTop, z - beamRadius, x + beamRadius, yBottom, z - beamRadius);
        addQuad(consumer, pose, x + beamRadius, yBottom, z - beamRadius, x + beamRadius, yTop, z - beamRadius, x + beamRadius, yTop, z + beamRadius, x + beamRadius, yBottom, z + beamRadius);
        addQuad(consumer, pose, x + beamRadius, yBottom, z + beamRadius, x + beamRadius, yTop, z + beamRadius, x - beamRadius, yTop, z + beamRadius, x - beamRadius, yBottom, z + beamRadius);
        addQuad(consumer, pose, x - beamRadius, yBottom, z + beamRadius, x - beamRadius, yTop, z + beamRadius, x - beamRadius, yTop, z - beamRadius, x - beamRadius, yBottom, z - beamRadius);
    }

    private static void renderShockwave(PoseStack poseStack, VertexConsumer consumer, Vec3 targetPos) {
        Matrix4f pose = poseStack.last().pose();
        float radius = 8.0F;
        float innerRadius = radius * 0.6F;
        float y = (float) targetPos.y + 0.1F;
        int segments = 32;
        float centerX = (float) targetPos.x;
        float centerZ = (float) targetPos.z;
        for (int i = 0; i < segments; i++) {
            float angle0 = (float) (2.0F * Math.PI * i / segments);
            float angle1 = (float) (2.0F * Math.PI * (i + 1) / segments);
            float x0Inner = centerX + innerRadius * Mth.cos(angle0);
            float z0Inner = centerZ + innerRadius * Mth.sin(angle0);
            float x1Inner = centerX + innerRadius * Mth.cos(angle1);
            float z1Inner = centerZ + innerRadius * Mth.sin(angle1);
            float x0Outer = centerX + radius * Mth.cos(angle0);
            float z0Outer = centerZ + radius * Mth.sin(angle0);
            float x1Outer = centerX + radius * Mth.cos(angle1);
            float z1Outer = centerZ + radius * Mth.sin(angle1);
            addQuad(consumer, pose, x0Inner, y, z0Inner, x1Inner, y, z1Inner, x1Outer, y, z1Outer, x0Outer, y, z0Outer);
        }
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        consumer.vertex(pose, x0, y0, z0).color(255, 255, 255, 255).uv(0.0F, 0.0F).endVertex();
        consumer.vertex(pose, x1, y1, z1).color(255, 255, 255, 255).uv(0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(255, 255, 255, 255).uv(1.0F, 1.0F).endVertex();
        consumer.vertex(pose, x3, y3, z3).color(255, 255, 255, 255).uv(1.0F, 0.0F).endVertex();
    }

    private static void setUniform(Uniform uniform, float value) {
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setUniform(Uniform uniform, int value) {
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setUniform(Uniform uniform, float v0, float v1) {
        if (uniform != null) {
            uniform.set(v0, v1);
        }
    }

    private static void setUniform(Uniform uniform, float v0, float v1, float v2) {
        if (uniform != null) {
            uniform.set(v0, v1, v2);
        }
    }
}

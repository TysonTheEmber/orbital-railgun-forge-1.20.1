package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;

import java.io.IOException;

public final class RailgunFxRenderer {
    private static ShaderInstance screenDistortShader;
    private static ShaderInstance screenTintShader;
    private static ShaderInstance beamShader;

    private RailgunFxRenderer() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ForgeOrbitalRailgunMod.id("orbital_screen_distort"), DefaultVertexFormat.POSITION), shader -> screenDistortShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ForgeOrbitalRailgunMod.id("orbital_screen_tint"), DefaultVertexFormat.POSITION), shader -> screenTintShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ForgeOrbitalRailgunMod.id("orbital_beam"), DefaultVertexFormat.POSITION), shader -> beamShader = shader);
    }

    public static void renderBeams(RenderLevelStageEvent event, RailgunState state) {
        if (beamShader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        boolean strikeActive = state.isStrikeActive() && state.getStrikeDimension() != null && state.getStrikeDimension().equals(minecraft.level.dimension());
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
        }

        float partialTick = event.getPartialTick();
        float timeSeconds = strikeActive ? state.getStrikeSeconds(partialTick) : state.getChargeSeconds(partialTick);
        Vec3 targetPos = strikeActive ? state.getStrikePos() : state.getHitPos();
        if (targetPos == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        float distance = (float) cameraPos.distanceTo(targetPos);

        float flash = strikeActive ? computeStrikeFlash(state, partialTick) : computeChargeFlash(state, partialTick);
        setCommonUniforms(beamShader, timeSeconds, flash, state, targetPos, distance);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f pose = poseStack.last().pose();

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        var consumer = bufferSource.getBuffer(RailgunRenderTypes.BEAM_ADDITIVE);

        float radius = 6.0F + Mth.sin(timeSeconds * 0.75F) * 2.0F;
        float height = 300.0F;
        float baseY = (float) targetPos.y;
        float x = (float) targetPos.x;
        float z = (float) targetPos.z;

        consumer.vertex(pose, x - radius, baseY, z - radius).endVertex();
        consumer.vertex(pose, x - radius, baseY + height, z - radius).endVertex();
        consumer.vertex(pose, x + radius, baseY, z + radius).endVertex();
        consumer.vertex(pose, x + radius, baseY + height, z + radius).endVertex();

        bufferSource.endBatch(RailgunRenderTypes.BEAM_ADDITIVE);
        poseStack.popPose();
    }

    public static void renderScreenFx(RenderLevelStageEvent event, RailgunState state, float partialTick) {
        if (screenDistortShader == null || screenTintShader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null) {
            return;
        }

        boolean strikeActive = state.isStrikeActive() && minecraft.level != null && state.getStrikeDimension() != null && state.getStrikeDimension().equals(minecraft.level.dimension());
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
        }

        Vec3 targetPos = strikeActive ? state.getStrikePos() : state.getHitPos();
        if (targetPos == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        float distance = (float) cameraPos.distanceTo(targetPos);
        float timeSeconds = strikeActive ? state.getStrikeSeconds(partialTick) : state.getChargeSeconds(partialTick);
        float flash = strikeActive ? computeStrikeFlash(state, partialTick) : computeChargeFlash(state, partialTick);

        setCommonUniforms(screenDistortShader, timeSeconds, flash, state, targetPos, distance);
        setCommonUniforms(screenTintShader, timeSeconds, flash, state, targetPos, distance);

        renderFullscreenQuad(screenDistortShader);
        renderFullscreenQuad(screenTintShader);
    }

    private static void renderFullscreenQuad(ShaderInstance shader) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        builder.vertex(-1.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(-1.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(1.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(1.0F, 1.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static float computeStrikeFlash(RailgunState state, float partialTick) {
        float seconds = state.getStrikeSeconds(partialTick);
        return Mth.clamp(1.5F - seconds * 0.25F, 0.0F, 1.0F);
    }

    private static float computeChargeFlash(RailgunState state, float partialTick) {
        float seconds = state.getChargeSeconds(partialTick);
        return Mth.clamp(seconds * 0.25F, 0.0F, 1.0F);
    }

    private static void setCommonUniforms(ShaderInstance shader, float timeSeconds, float flashValue, RailgunState state, Vec3 targetPos, float distance) {
        if (shader == null) {
            return;
        }
        if (shader.getUniform("Time") != null) {
            shader.getUniform("Time").set(timeSeconds);
        }
        if (shader.getUniform("Flash01") != null) {
            shader.getUniform("Flash01").set(flashValue);
        }
        if (shader.getUniform("HitKind") != null) {
            shader.getUniform("HitKind").set(state.getHitKind().ordinal());
        }
        if (shader.getUniform("HitPos") != null) {
            shader.getUniform("HitPos").set((float) targetPos.x, (float) targetPos.y, (float) targetPos.z);
        }
        if (shader.getUniform("Distance") != null) {
            shader.getUniform("Distance").set(distance);
        }
        if (shader.getUniform("HasGrab") != null) {
            shader.getUniform("HasGrab").set(0);
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget != null && shader.getUniform("ScreenSize") != null) {
            shader.getUniform("ScreenSize").set((float) renderTarget.width, (float) renderTarget.height);
        }
    }

    public static boolean hasShaders() {
        return screenDistortShader != null && screenTintShader != null && beamShader != null;
    }
}

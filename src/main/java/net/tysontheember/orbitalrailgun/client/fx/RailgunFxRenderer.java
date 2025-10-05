package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;

import java.util.Optional;
import org.joml.Matrix4f;

public final class RailgunFxRenderer {
    private RailgunFxRenderer() {
    }

    public static void renderBeams(RenderLevelStageEvent event, RailgunState state) {
        if (!shouldRender(state)) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 target = state.isStrikeActive() ? state.getStrikePos() : state.getHitPos();
        if (target == null) {
            return;
        }

        float partialTicks = event.getPartialTick();
        float time = state.isStrikeActive() ? state.getStrikeSeconds(partialTicks) : state.getChargeSeconds(partialTicks);
        float flash = computeFlash(state, partialTicks);

        RenderType renderType = RailgunRenderTypes.BEAM_ADDITIVE;
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> GameRenderer.getShader("orbital_railgun:orbital_beam"));

        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            setShaderUniform(shader, "Time", time);
            setShaderUniform(shader, "Flash01", flash);
            setShaderUniform(shader, "Distance", (float) cameraPos.distanceTo(target));
            setShaderUniform(shader, "HitKind", state.getHitKind().ordinal());
            setShaderUniform(shader, "HitPos", target);
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f modelView = new Matrix4f(poseStack.last().pose());
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        if (shader != null) {
            setShaderUniform(shader, "ModelViewMat", modelView);
            setShaderUniform(shader, "ProjMat", projection);
        }

        Vec3 start = target.add(0.0, 260.0, 0.0);
        Vec3 end = target.subtract(0.0, 32.0, 0.0);
        Vec3 forward = end.subtract(start);
        float length = (float) forward.length();
        Vec3 dir = forward.normalize();
        Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-4) {
            side = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        side = side.normalize().scale(6.0);
        Vec3 up = dir.cross(side).normalize().scale(6.0);

        builder.begin(renderType.mode(), renderType.format());
        submitQuad(builder, poseStack, start, end, side, up, flash);
        BufferUploader.drawWithShader(builder.end());

        poseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    public static void renderScreenFx(RenderLevelStageEvent event, RailgunState state, float partialTicks) {
        if (!shouldRender(state)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getMainRenderTarget() == null) {
            return;
        }

        float time = state.isStrikeActive() ? state.getStrikeSeconds(partialTicks) : state.getChargeSeconds(partialTicks);
        float flash = computeFlash(state, partialTicks);
        Vec3 hitPos = state.isStrikeActive() ? state.getStrikePos() : state.getHitPos();
        float distance = hitPos == null ? 0.0F : (float) minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(hitPos);

        renderFullscreenPass(minecraft, RailgunRenderTypes.SCREEN_FX_DISTORT, time, flash, state, hitPos, distance, true);
        renderFullscreenPass(minecraft, RailgunRenderTypes.SCREEN_FX_TINT, time, flash, state, hitPos, distance, false);
    }

    private static void renderFullscreenPass(Minecraft minecraft, RenderType renderType, float time, float flash, RailgunState state,
                                             Vec3 hitPos, float distance, boolean distort) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> GameRenderer.getShader(distort ? "orbital_railgun:orbital_screen_distort" : "orbital_railgun:orbital_screen_tint"));

        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            setShaderUniform(shader, "Time", time);
            setShaderUniform(shader, "Flash01", flash);
            setShaderUniform(shader, "HitKind", state.getHitKind().ordinal());
            setShaderUniform(shader, "Distance", distance);
            if (hitPos != null) {
                setShaderUniform(shader, "HitPos", hitPos);
            }
            setShaderUniform(shader, "HasGrab", 0);
            setShaderUniform(shader, "ScreenSize", minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(renderType.mode(), renderType.format());
        builder.vertex(-1.0, -1.0, 0.0).endVertex();
        builder.vertex(3.0, -1.0, 0.0).endVertex();
        builder.vertex(-1.0, 3.0, 0.0).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void submitQuad(BufferBuilder builder, PoseStack poseStack, Vec3 start, Vec3 end, Vec3 side, Vec3 up, float flash) {
        PoseStack.Pose pose = poseStack.last();
        float alpha = Mth.clamp(flash, 0.0F, 1.0F);

        putVertex(builder, pose, start.subtract(side).subtract(up), alpha);
        putVertex(builder, pose, start.add(side).subtract(up), alpha);
        putVertex(builder, pose, end.add(side).add(up), alpha);
        putVertex(builder, pose, end.subtract(side).add(up), alpha);
    }

    private static void putVertex(BufferBuilder builder, PoseStack.Pose pose, Vec3 position, float alpha) {
        builder.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
            .color(1.0F, 1.0F, 1.0F, alpha)
            .endVertex();
    }

    private static void setShaderUniform(ShaderInstance shader, String name, float value) {
        Optional.ofNullable(shader.getUniform(name)).ifPresent(uniform -> uniform.set(value));
    }

    private static void setShaderUniform(ShaderInstance shader, String name, int value) {
        Optional.ofNullable(shader.getUniform(name)).ifPresent(uniform -> uniform.set(value));
    }

    private static void setShaderUniform(ShaderInstance shader, String name, float x, float y) {
        Optional.ofNullable(shader.getUniform(name)).ifPresent(uniform -> uniform.set(x, y));
    }

    private static void setShaderUniform(ShaderInstance shader, String name, Vec3 vec) {
        Optional.ofNullable(shader.getUniform(name)).ifPresent(uniform -> uniform.set((float) vec.x, (float) vec.y, (float) vec.z));
    }

    private static void setShaderUniform(ShaderInstance shader, String name, Matrix4f matrix) {
        Optional.ofNullable(shader.getUniform(name)).ifPresent(uniform -> uniform.set(matrix));
    }

    private static boolean shouldRender(RailgunState state) {
        return state.isStrikeActive() || state.isCharging();
    }

    private static float computeFlash(RailgunState state, float partialTicks) {
        if (state.isStrikeActive()) {
            return 1.0F;
        }
        return Mth.clamp(state.getChargeSeconds(partialTicks) / 2.0F, 0.0F, 1.0F);
    }
}

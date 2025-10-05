package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class RailgunFxRenderer {
    public static ShaderInstance SCREEN_DISTORT;
    public static ShaderInstance SCREEN_TINT;
    public static ShaderInstance BEAM;

    private RailgunFxRenderer() {}

    public static SimplePreparableReloadListener<Void> createReloadListener() {
        return new SimplePreparableReloadListener<Void>() {
            @Override
            protected @Nullable Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null; // nothing to preload
            }

            @Override
            protected void apply(@Nullable Void prepped, ResourceManager resourceManager, ProfilerFiller profiler) {
                // no-op: shaders are registered via RegisterShadersEvent
            }
        };
    }

    public static void renderBeams(RenderLevelStageEvent event, RailgunState state, float partialTick) {
        if (BEAM == null) {
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

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 effectPos = strikeActive ? state.getStrikePos() : state.getHitPos();
        if (effectPos.equals(Vec3.ZERO)) {
            var lookV3f = camera.getLookVector();
            Vec3 look = new Vec3(lookV3f.x(), lookV3f.y(), lookV3f.z());
            effectPos = cameraPos.add(look.scale(64.0D));
        }

        float timeSeconds = strikeActive ? state.getStrikeSeconds(partialTick) : state.getChargeSeconds(partialTick);
        float flashStrength = strikeActive ? Mth.clamp(1.0F - timeSeconds / 5.0F, 0.0F, 1.0F) : (chargeActive ? 0.15F : 0.0F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        applyCommonUniforms(BEAM, state, partialTick, strikeActive, timeSeconds, effectPos, flashStrength);
        setMatrix(BEAM, "ProjMat", event.getProjectionMatrix());
        setMatrix(BEAM, "ModelViewMat", poseStack.last().pose());

        Matrix4f poseMatrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        RenderSystem.setShader(() -> BEAM);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 forward = effectPos.subtract(cameraPos).normalize();
        Vec3 right = forward.cross(up).normalize();
        if (right.lengthSqr() < 1.0E-4D) {
            var leftV3f = camera.getLeftVector();
            right = new Vec3(-leftV3f.x(), -leftV3f.y(), -leftV3f.z());
        }
        right = right.normalize().scale(6.0D);

        Vec3 top = effectPos.add(0.0D, 320.0D, 0.0D);
        Vec3 bottom = effectPos.add(0.0D, -16.0D, 0.0D);

        Vec3 topLeft = top.subtract(right);
        Vec3 topRight = top.add(right);
        Vec3 bottomLeft = bottom.subtract(right);
        Vec3 bottomRight = bottom.add(right);

        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        putVertex(builder, poseMatrix, bottomLeft);
        putVertex(builder, poseMatrix, bottomRight);
        putVertex(builder, poseMatrix, topRight);

        putVertex(builder, poseMatrix, bottomLeft);
        putVertex(builder, poseMatrix, topRight);
        putVertex(builder, poseMatrix, topLeft);

        BufferBuilder.RenderedBuffer buffer = builder.end();
        BufferUploader.drawWithShader(buffer);
        buffer.release();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static void putVertex(BufferBuilder builder, Matrix4f matrix, Vec3 pos) {
        builder.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).endVertex();
    }

    public static void renderScreenFx(RenderLevelStageEvent event, RailgunState state, float partialTick) {
        if (SCREEN_DISTORT == null || SCREEN_TINT == null || BEAM == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        if (window == null) {
            return;
        }

        boolean strikeActive = state.isStrikeActive();
        boolean chargeActive = state.isCharging();
        if (!strikeActive && !chargeActive) {
            return;
        }

        Vec3 effectPos = strikeActive ? state.getStrikePos() : state.getHitPos();
        if (effectPos.equals(Vec3.ZERO)) {
            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();
            var lookV3f = camera.getLookVector();
            Vec3 look = new Vec3(lookV3f.x(), lookV3f.y(), lookV3f.z());
            effectPos = camPos.add(look.scale(64.0D));
        }

        float timeSeconds = strikeActive ? state.getStrikeSeconds(partialTick) : state.getChargeSeconds(partialTick);
        float flashStrength = strikeActive ? Mth.clamp(1.0F - timeSeconds / 3.0F, 0.0F, 1.0F) : (chargeActive ? 0.1F : 0.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(() -> SCREEN_DISTORT);
        applyCommonUniforms(SCREEN_DISTORT, state, partialTick, strikeActive, timeSeconds, effectPos, flashStrength);
        setScreenUniforms(SCREEN_DISTORT, window, false);
        drawFullscreenQuad();

        RenderSystem.setShader(() -> SCREEN_TINT);
        applyCommonUniforms(SCREEN_TINT, state, partialTick, strikeActive, timeSeconds, effectPos, flashStrength);
        setScreenUniforms(SCREEN_TINT, window, false);
        drawFullscreenQuad();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void setScreenUniforms(ShaderInstance shader, Window window, boolean hasGrab) {
        setFloat(shader, "HasGrab", hasGrab ? 1.0F : 0.0F);
        setVec2(shader, "ScreenSize", (float) window.getWidth(), (float) window.getHeight());
    }

    private static void drawFullscreenQuad() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        builder.vertex(-1.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(1.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(1.0F, 1.0F, 0.0F).endVertex();

        builder.vertex(-1.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(1.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(-1.0F, 1.0F, 0.0F).endVertex();
        BufferBuilder.RenderedBuffer buffer = builder.end();
        BufferUploader.drawWithShader(buffer);
        buffer.release();
    }

    private static void applyCommonUniforms(ShaderInstance shader, RailgunState state, float partialTick, boolean strikeActive, float timeSeconds, Vec3 effectPos, float flashStrength) {
        Vec3 hitPos = state.getHitPos();
        float distance = state.getHitDistance();
        if (strikeActive) {
            hitPos = state.getStrikePos();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getCameraEntity() != null) {
                distance = (float) minecraft.getCameraEntity().position().distanceTo(hitPos);
            }
        }

        setFloat(shader, "Time", timeSeconds);
        setFloat(shader, "Flash01", flashStrength);
        setInt(shader, "HitKind", state.getHitKind().ordinal());
        setVec3(shader, "HitPos", hitPos);
        setFloat(shader, "Distance", distance);
        setVec3(shader, "EffectPos", effectPos);
    }

    private static void setMatrix(ShaderInstance shader, String name, Matrix4f matrix) {
        if (shader == null) {
            return;
        }
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private static void setFloat(ShaderInstance shader, String name, float value) {
        if (shader == null) {
            return;
        }
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setInt(ShaderInstance shader, String name, int value) {
        if (shader == null) {
            return;
        }
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setVec2(ShaderInstance shader, String name, float x, float y) {
        if (shader == null) {
            return;
        }
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setVec3(ShaderInstance shader, String name, Vec3 vec) {
        if (shader == null) {
            return;
        }
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }
}

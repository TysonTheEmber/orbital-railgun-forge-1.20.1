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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import org.joml.Matrix4f;

import java.io.IOException;

public final class RailgunFxRenderer {
    private static ShaderInstance screenDistortShader;
    private static ShaderInstance screenTintShader;
    private static ShaderInstance beamShader;

    private RailgunFxRenderer() {}

    public static SimplePreparableReloadListener<Void> createReloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Void prepare(ResourceManager resourceManager) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager) {
                reloadShaders(resourceManager);
            }
        };
    }

    private static void reloadShaders(ResourceManager resourceManager) {
        closeShaders();
        try {
            screenDistortShader = new ShaderInstance(resourceManager, ForgeOrbitalRailgunMod.id("orbital_screen_distort"));
            screenTintShader = new ShaderInstance(resourceManager, ForgeOrbitalRailgunMod.id("orbital_screen_tint"));
            beamShader = new ShaderInstance(resourceManager, ForgeOrbitalRailgunMod.id("orbital_beam"));
            ForgeOrbitalRailgunMod.LOGGER.debug("Reloaded orbital railgun Iris-compatible shaders");
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun shaders", exception);
            closeShaders();
        }
    }

    private static void closeShaders() {
        if (screenDistortShader != null) {
            screenDistortShader.close();
            screenDistortShader = null;
        }
        if (screenTintShader != null) {
            screenTintShader.close();
            screenTintShader = null;
        }
        if (beamShader != null) {
            beamShader.close();
            beamShader = null;
        }
    }

    public static void renderBeams(RenderLevelStageEvent event, RailgunState state, float partialTick) {
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

        Vec3 cameraPos = event.getCamera().getPosition();
        Vec3 effectPos = strikeActive ? state.getStrikePos() : state.getHitPos();
        if (effectPos.equals(Vec3.ZERO)) {
            effectPos = cameraPos.add(event.getCamera().getLookVector().scale(64.0D));
        }

        float timeSeconds = strikeActive ? state.getStrikeSeconds(partialTick) : state.getChargeSeconds(partialTick);
        float flashStrength = strikeActive ? Mth.clamp(1.0F - timeSeconds / 5.0F, 0.0F, 1.0F) : (chargeActive ? 0.15F : 0.0F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        applyCommonUniforms(beamShader, state, partialTick, strikeActive, timeSeconds, effectPos, flashStrength);
        setMatrix(beamShader, "ProjMat", event.getProjectionMatrix());
        setMatrix(beamShader, "ModelViewMat", poseStack.last().pose());

        Matrix4f poseMatrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        RenderSystem.setShader(() -> beamShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 forward = effectPos.subtract(cameraPos).normalize();
        Vec3 right = forward.cross(up).normalize();
        if (right.lengthSqr() < 1.0E-4D) {
            right = event.getCamera().getLeftVector();
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
        if (screenDistortShader == null || screenTintShader == null) {
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
            effectPos = camera.getPosition().add(camera.getLookVector().scale(64.0D));
        }

        float timeSeconds = strikeActive ? state.getStrikeSeconds(partialTick) : state.getChargeSeconds(partialTick);
        float flashStrength = strikeActive ? Mth.clamp(1.0F - timeSeconds / 3.0F, 0.0F, 1.0F) : (chargeActive ? 0.1F : 0.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(() -> screenDistortShader);
        applyCommonUniforms(screenDistortShader, state, partialTick, strikeActive, timeSeconds, effectPos, flashStrength);
        setScreenUniforms(screenDistortShader, window, false);
        drawFullscreenQuad();

        RenderSystem.setShader(() -> screenTintShader);
        applyCommonUniforms(screenTintShader, state, partialTick, strikeActive, timeSeconds, effectPos, flashStrength);
        setScreenUniforms(screenTintShader, window, false);
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

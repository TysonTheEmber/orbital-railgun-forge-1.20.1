package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;

public final class OrbitalStrikeRenderer {
    private OrbitalStrikeRenderer() {
    }

    public static void renderBeamAndMarker(PoseStack poseStack, MultiBufferSource buffers, RailgunState state, float partialTick, Vec3 cameraPos) {
        Vec3 strikePos = state.getStrikePos();
        Vec3 hitPos = state.getHitPos();
        Vec3 look = getCameraLook(partialTick);

        Vec3 target = state.isStrikeActive() ? strikePos : hitPos;
        if (target.equals(Vec3.ZERO)) {
            target = cameraPos.add(look.scale(64.0D));
        }

        Vec3 source = state.isStrikeActive() ? strikePos.add(0.0D, 160.0D, 0.0D) : cameraPos;

        float charge = state.isCharging() ? Mth.clamp(state.getChargeSeconds(partialTick) / 3.0F, 0.0F, 1.0F) : 0.0F;
        float hitKind = switch (state.getHitKind()) {
            case NONE -> 0.0F;
            case BLOCK -> 0.5F;
            case ENTITY -> 1.0F;
        };
        float distance = (float) (state.isStrikeActive() ? cameraPos.distanceTo(strikePos) : state.getHitDistance());
        float distanceNorm = Mth.clamp(distance / 128.0F, 0.0F, 1.0F);
        float intensity = state.isStrikeActive() ? Mth.clamp(state.getStrikeSeconds(partialTick) / 6.0F, 0.0F, 1.0F) : charge;

        int rgba = packColor(charge, hitKind, distanceNorm, intensity);

        VertexConsumer beamConsumer = buffers.getBuffer(OrbRenderTypes.beam(ClientRender.MASK_TEX));
        BeamBuilder.build(poseStack, beamConsumer, source, target, rgba);

        VertexConsumer markerConsumer = buffers.getBuffer(OrbRenderTypes.marker(ClientRender.MASK_TEX));
        MarkerBuilder.build(poseStack, markerConsumer, source, target, rgba);
    }

    private static int packColor(float r, float g, float b, float a) {
        int ri = Mth.clamp((int) Math.round(r * 255.0F), 0, 255);
        int gi = Mth.clamp((int) Math.round(g * 255.0F), 0, 255);
        int bi = Mth.clamp((int) Math.round(b * 255.0F), 0, 255);
        int ai = Mth.clamp((int) Math.round(a * 255.0F), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static Vec3 getCameraLook(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity != null) {
            return cameraEntity.getViewVector(partialTick);
        }
        return new Vec3(0.0D, 0.0D, -1.0D);
    }
}

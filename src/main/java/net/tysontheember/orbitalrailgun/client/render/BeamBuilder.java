package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BeamBuilder {
    private static final double WIDTH = 0.35D;

    private BeamBuilder() {
    }

    public static void build(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to, int rgba) {
        Vec3 direction = to.subtract(from);
        double lengthSq = direction.lengthSqr();
        if (lengthSq <= 1.0E-6D) {
            return;
        }

        Vec3 forward = direction.normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() <= 1.0E-6D) {
            up = new Vec3(1.0D, 0.0D, 0.0D);
            right = forward.cross(up);
        }
        right = right.normalize().scale(WIDTH);

        Vec3 cornerA = from.add(right);
        Vec3 cornerB = from.subtract(right);
        Vec3 cornerC = to.subtract(right);
        Vec3 cornerD = to.add(right);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        float vMax = (float) Math.sqrt(lengthSq);

        vertex(consumer, matrix, cornerA, rgba, 0.0F, 0.0F);
        vertex(consumer, matrix, cornerB, rgba, 1.0F, 0.0F);
        vertex(consumer, matrix, cornerC, rgba, 1.0F, vMax);
        vertex(consumer, matrix, cornerD, rgba, 0.0F, vMax);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, int rgba, float u, float v) {
        int a = rgba >>> 24 & 255;
        int r = rgba >>> 16 & 255;
        int g = rgba >>> 8 & 255;
        int b = rgba & 255;
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, a)
                .uv(u, v)
                .uv2(0, 240)
                .endVertex();
    }
}

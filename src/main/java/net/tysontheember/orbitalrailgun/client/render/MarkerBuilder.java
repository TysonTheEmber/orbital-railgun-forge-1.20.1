package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MarkerBuilder {
    private MarkerBuilder() {
    }

    public static void build(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to, int rgba) {
        Vec3 direction = to.subtract(from);
        double lengthSq = direction.lengthSqr();
        if (lengthSq <= 1.0E-6D) {
            return;
        }

        Vec3 forward = direction.normalize();
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() <= 1.0E-6D) {
            side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();

        double radius = Math.min(32.0D, Math.sqrt(lengthSq) * 0.5D + 6.0D);
        Vec3 rightScaled = side.scale(radius);
        Vec3 upScaled = up.scale(radius);

        Vec3 v0 = to.add(rightScaled).add(upScaled);
        Vec3 v1 = to.subtract(rightScaled).add(upScaled);
        Vec3 v2 = to.subtract(rightScaled).subtract(upScaled);
        Vec3 v3 = to.add(rightScaled).subtract(upScaled);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        vertex(consumer, matrix, v0, rgba, 0.0F, 0.0F);
        vertex(consumer, matrix, v1, rgba, 1.0F, 0.0F);
        vertex(consumer, matrix, v2, rgba, 1.0F, 1.0F);
        vertex(consumer, matrix, v3, rgba, 0.0F, 1.0F);
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

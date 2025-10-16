package net.tysontheember.orbitalrailgun.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** Client-only raycast helper that mirrors the Fabric behaviour and keeps a sticky block target. */
public final class ClientRaycast {
    private static final double RANGE = 128.0D;
    private static BlockPos lastValidBlock = BlockPos.ZERO;

    private ClientRaycast() {}

    @NotNull
    public static BlockPos pickBlockPos(Minecraft minecraft, float partialTick) {
        if (minecraft == null) {
            return lastValidBlock;
        }

        Level level = minecraft.level;
        if (level == null) {
            return lastValidBlock;
        }

        var cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return lastValidBlock;
        }

        Vec3 start = cameraEntity.getEyePosition(partialTick);
        Vec3 look = cameraEntity.getViewVector(partialTick);
        Vec3 end = start.add(look.scale(RANGE));

        ClipContext context = new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cameraEntity);
        HitResult hitResult = level.clip(context);

        if (hitResult instanceof BlockHitResult blockHit) {
            lastValidBlock = blockHit.getBlockPos();
        }

        return lastValidBlock;
    }

    public static void reset() {
        lastValidBlock = BlockPos.ZERO;
    }
}

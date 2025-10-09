package net.tysontheember.orbitalrailgun.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;

public final class OPACGuard {
    private OPACGuard() {
    }

    public static boolean canBreakBlock(ServerLevel level, ServerPlayer shooter, BlockPos pos) {
        if (!OrbitalConfig.ALLOW_BLOCK_BREAK_IN_CLAIMS.get()) {
            return false;
        }
        try {
            var api = xaero.pac.common.server.api.OpenPACServerAPI.get(level.getServer());
            if (api == null) {
                return true;
            }
            var protection = api.getChunkProtection();
            if (protection == null) {
                return true;
            }
            boolean protect = protection.onEntityPlaceBlock(shooter, level, pos);
            return !protect;
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean canDamageEntity(ServerLevel level, ServerPlayer shooter, Entity target) {
        if (!OrbitalConfig.ALLOW_ENTITY_DAMAGE_IN_CLAIMS.get()) {
            return false;
        }
        try {
            var api = xaero.pac.common.server.api.OpenPACServerAPI.get(level.getServer());
            if (api == null) {
                return true;
            }
            var protection = api.getChunkProtection();
            if (protection == null) {
                return true;
            }
            boolean protect = protection.onEntityInteraction(
                shooter, shooter, target,
                shooter.getMainHandItem(), InteractionHand.MAIN_HAND,
                true,
                false,
                true
            );
            return !protect;
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean canAffectPosFromPos(ServerLevel fromLevel, ChunkPos fromChunk, ServerLevel toLevel, ChunkPos toChunk) {
        if (!OrbitalConfig.ALLOW_EXPLOSIONS_IN_CLAIMS.get()) {
            return false;
        }
        try {
            var api = xaero.pac.common.server.api.OpenPACServerAPI.get(fromLevel.getServer());
            if (api == null) {
                return true;
            }
            var protection = api.getChunkProtection();
            if (protection == null) {
                return true;
            }
            boolean protect = protection.onPosAffectedByAnotherPos(
                toLevel, toChunk,
                fromLevel, fromChunk,
                true,
                true,
                true
            );
            return !protect;
        } catch (Throwable t) {
            return true;
        }
    }
}

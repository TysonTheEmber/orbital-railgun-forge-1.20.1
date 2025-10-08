package net.tysontheember.orbitalrailgun.compat;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class ClaimQuery {
    private ClaimQuery() {
    }

    private static ClaimedChunkManager getManager() {
        try {
            FTBChunksAPI.API api = FTBChunksAPI.api();
            if (api != null && api.isManagerLoaded()) {
                return api.getManager();
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    private static ClaimedChunk getChunk(ServerLevel level, BlockPos pos) {
        ClaimedChunkManager manager = getManager();
        if (manager == null) {
            return null;
        }
        return manager.getChunk(new ChunkDimPos(level, pos));
    }

    static boolean isClaimed(ServerLevel level, BlockPos pos) {
        return getChunk(level, pos) != null;
    }

    static boolean canEdit(ServerLevel level, BlockPos pos, ServerPlayer player) {
        ClaimedChunk chunk = getChunk(level, pos);
        if (chunk == null) {
            return true;
        }
        ChunkTeamData data = chunk.getTeamData();
        UUID playerId = player.getUUID();
        if (data.getManager().getBypassProtection(playerId)) {
            return true;
        }
        if (isMemberOrAlly(data, playerId)) {
            return true;
        }
        return data.canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_MODE);
    }

    static boolean canAttack(ServerLevel level, Entity target, ServerPlayer player) {
        ClaimedChunk chunk = getChunk(level, target.blockPosition());
        if (chunk == null) {
            return true;
        }
        ChunkTeamData data = chunk.getTeamData();
        UUID playerId = player.getUUID();
        if (data.getManager().getBypassProtection(playerId)) {
            return true;
        }
        if (isMemberOrAlly(data, playerId)) {
            return true;
        }
        if (target instanceof ServerPlayer) {
            return data.allowPVP();
        }
        return data.canPlayerUse(player, FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE);
    }

    static boolean canExplode(ServerLevel level, BlockPos pos, ServerPlayer player) {
        ClaimedChunk chunk = getChunk(level, pos);
        if (chunk == null) {
            return true;
        }
        ChunkTeamData data = chunk.getTeamData();
        UUID playerId = player.getUUID();
        if (data.getManager().getBypassProtection(playerId)) {
            return true;
        }
        if (isMemberOrAlly(data, playerId)) {
            return true;
        }
        return data.canExplosionsDamageTerrain();
    }

    private static boolean isMemberOrAlly(ChunkTeamData data, UUID playerId) {
        return data.isTeamMember(playerId) || data.isAlly(playerId);
    }
}

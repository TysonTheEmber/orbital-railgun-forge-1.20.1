package net.tysontheember.orbitalrailgun.compat;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;
import java.util.UUID;

final class ClaimQuery {
    private ClaimQuery() {}

    private static Optional<ClaimedChunk> get(ServerLevel level, BlockPos pos) {
        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkDimPos chunkDimPos = new ChunkDimPos(level.dimension(), chunkPos.x, chunkPos.z);
        return manager.getChunk(chunkDimPos);
    }

    static boolean isClaimed(ServerLevel level, BlockPos pos) {
        return get(level, pos).isPresent();
    }

    static boolean canEdit(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Optional<ClaimedChunk> opt = get(level, pos);
        if (opt.isEmpty()) {
            return true;
        }
        return isMemberOrAlly(opt.get(), player.getUUID());
    }

    static boolean canAttack(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Optional<ClaimedChunk> opt = get(level, pos);
        if (opt.isEmpty()) {
            return true;
        }
        return isMemberOrAlly(opt.get(), player.getUUID());
    }

    static boolean canExplode(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Optional<ClaimedChunk> opt = get(level, pos);
        if (opt.isEmpty()) {
            return true;
        }
        return isMemberOrAlly(opt.get(), player.getUUID());
    }

    private static boolean isMemberOrAlly(ClaimedChunk chunk, UUID playerId) {
        Team owner = chunk.getTeamData().getTeam();
        if (owner == null) {
            return true;
        }
        if (owner.isMember(playerId)) {
            return true;
        }
        return owner.isAlly(playerId);
    }
}

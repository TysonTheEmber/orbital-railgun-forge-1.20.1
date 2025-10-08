package net.tysontheember.orbitalrailgun.strike;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StrikeExecutor {
    private static final LongArrayFIFOQueue QUEUE = new LongArrayFIFOQueue();
    private static final LongOpenHashSet SEEN = new LongOpenHashSet();
    private static ServerLevel LEVEL;

    private StrikeExecutor() {}

    public static void begin(ServerLevel level, BlockPos impactCenter, double diameter) {
        LEVEL = level;
        QUEUE.clear();
        SEEN.clear();

        final double radius = Math.max(0.0, diameter * 0.5);
        final int r = (int) Math.ceil(radius);

        final int minY = level.getMinBuildHeight();
        final int maxY = level.getMaxBuildHeight() - 1;
        final int topY = maxY; // we enqueue everything; filterAllowed will prune to claims/depth

        for (int y = topY; y >= minY; --y) {
            final int baseX = impactCenter.getX();
            final int baseZ = impactCenter.getZ();
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        long key = BlockPos.asLong(baseX + dx, y, baseZ + dz);
                        if (SEEN.add(key)) {
                            QUEUE.enqueue(key);
                        }
                    }
                }
            }
        }
    }


    public static void filterAllowed(LongSet allowed) {
        if (LEVEL == null) return;

        LongArrayList ordered = new LongArrayList();
        while (!QUEUE.isEmpty()) {
            long key = QUEUE.dequeueLong();
            if (allowed.contains(key)) {
                ordered.add(key);
            }
        }
        QUEUE.clear();
        SEEN.clear();
        for (long key : ordered) {
            if (SEEN.add(key)) {
                QUEUE.enqueue(key);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || LEVEL == null) return;
        if (QUEUE.isEmpty()) { LEVEL = null; return; }

        int budget = OrbitalConfig.BLOCKS_PER_TICK.get();
        boolean drop = OrbitalConfig.DROP_BLOCKS.get();

        boolean allowUnbreakables = OrbitalConfig.MAX_BREAK_HARDNESS.get() < 0.0D;

        for (int i = 0; i < budget && !QUEUE.isEmpty(); i++) {
            long key = QUEUE.dequeueLong();
            BlockPos pos = BlockPos.of(key);

            if (!LEVEL.isLoaded(pos)) continue; // avoid force-loading
            BlockState state = LEVEL.getBlockState(pos);
            if (state.isAir()) continue;

            if (!allowUnbreakables && state.getDestroySpeed(LEVEL, pos) < 0) continue;

            if (drop) {
                LEVEL.destroyBlock(pos, true, null); // heavier (loot, entities)
            } else {
                LEVEL.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); // UPDATE_CLIENTS; lighter
            }
        }
    }
}

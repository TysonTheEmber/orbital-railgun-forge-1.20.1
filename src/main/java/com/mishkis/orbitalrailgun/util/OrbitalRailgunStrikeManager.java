package com.mishkis.orbitalrailgun.util;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mishkis.orbitalrailgun.network.Network;
import com.mishkis.orbitalrailgun.network.S2C_PlayStrikeEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OrbitalRailgunStrikeManager {
    private static final int RADIUS = 24;
    private static final int RADIUS_SQUARED = RADIUS * RADIUS;
    private static final boolean[][] MASK = new boolean[RADIUS * 2 + 1][RADIUS * 2 + 1];
    private static final ResourceKey<DamageType> STRIKE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ForgeOrbitalRailgunMod.id("strike"));

    private static final Map<StrikeKey, ActiveStrike> ACTIVE_STRIKES = new ConcurrentHashMap<>();

    static {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                MASK[x + RADIUS][z + RADIUS] = Vector2i.lengthSquared(x, z) <= RADIUS_SQUARED;
            }
        }
    }

    private OrbitalRailgunStrikeManager() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(OrbitalRailgunStrikeManager::onServerTick);
    }

    public static void startStrike(ServerPlayer player, BlockPos target) {
        ServerLevel serverLevel = player.serverLevel();
        if (serverLevel.isClientSide()) {
            return;
        }
        List<Entity> tracked = new ArrayList<>(serverLevel.getEntities(null, AABB.ofSize(Vec3.atCenterOf(target), 1000.0D, 1000.0D, 1000.0D)));
        StrikeKey key = new StrikeKey(serverLevel.dimension(), target.immutable());
        ACTIVE_STRIKES.put(key, new ActiveStrike(key, tracked, player.getServer().getTickCount()));

        serverLevel.playSound(null, target, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 8.0F, 0.6F);
        Network.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(target.getX(), target.getY(), target.getZ(), 512.0D, serverLevel.dimension())), new S2C_PlayStrikeEffects(target, serverLevel.dimension()));
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Iterator<Map.Entry<StrikeKey, ActiveStrike>> iterator = ACTIVE_STRIKES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<StrikeKey, ActiveStrike> entry = iterator.next();
            ActiveStrike strike = entry.getValue();
            Level level = event.getServer().getLevel(strike.key.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            int age = event.getServer().getTickCount() - strike.startTick;
            if (age >= 700) {
                iterator.remove();
                damageEntities(level, strike, age);
                explode(level, strike.key.pos());
            } else if (age >= 400) {
                pushEntities(level, strike, age);
            }
        }
    }

    private static void pushEntities(Level level, ActiveStrike strike, int age) {
        Vec3 center = Vec3.atCenterOf(strike.key.pos());
        for (Entity entity : strike.entities) {
            if (entity == null || !entity.isAlive() || entity.level() != level) {
                continue;
            }
            if (entity instanceof Player player && player.isSpectator()) {
                continue;
            }
            Vec3 direction = center.subtract(entity.position());
            double length = direction.length();
            if (length < 1e-4) {
                continue;
            }
            double magnitude = Math.min(1.0 / Math.max(Math.abs(length - 20.0), 0.001) * 4.0 * (age - 400.0) / 300.0, 5.0);
            Vec3 velocity = direction.normalize().scale(magnitude);
            entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));
            entity.hurtMarked = true;
        }
    }

    private static void damageEntities(Level level, ActiveStrike strike, int age) {
        Vec3 center = Vec3.atCenterOf(strike.key.pos());
        Holder<DamageType> damageType = level.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(STRIKE_DAMAGE);
        DamageSource source = new DamageSource(damageType);
        for (Entity entity : strike.entities) {
            if (entity == null || !entity.isAlive() || entity.level() != level) {
                continue;
            }
            if (entity.position().distanceToSqr(center) <= RADIUS_SQUARED) {
                entity.hurt(source, 100000.0F);
            }
        }
    }

    private static void explode(Level level, BlockPos center) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (MASK[x + RADIUS][z + RADIUS]) {
                        mutable.set(center.getX() + x, y, center.getZ() + z);
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private record StrikeKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final class ActiveStrike {
        private final StrikeKey key;
        private final List<Entity> entities;
        private final int startTick;

        private ActiveStrike(StrikeKey key, List<Entity> entities, int startTick) {
            this.key = key;
            this.entities = entities;
            this.startTick = startTick;
        }
    }
}

package net.tysontheember.orbitalrailgun.util;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.compat.FTBChunksCompat;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;
import net.tysontheember.orbitalrailgun.registry.ModSounds;
import net.tysontheember.orbitalrailgun.network.Network;
import net.tysontheember.orbitalrailgun.network.S2C_PlayStrikeEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public final class OrbitalRailgunStrikeManager {
    private static final int RADIUS = 24;
    private static final int RADIUS_SQUARED = RADIUS * RADIUS;
    private static final boolean[][] MASK = new boolean[RADIUS * 2 + 1][RADIUS * 2 + 1];
    private static final ResourceKey<DamageType> STRIKE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ForgeOrbitalRailgunMod.id("strike"));

    private static final Map<StrikeKey, ActiveStrike> ACTIVE_STRIKES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_BLOCKED_MESSAGE_TICK = new ConcurrentHashMap<>();

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
        ACTIVE_STRIKES.put(key, new ActiveStrike(key, tracked, player.getServer().getTickCount(), player.getUUID()));

        if (ModSounds.RAILGUN_SHOOT.isPresent()) {
            serverLevel.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.RAILGUN_SHOOT.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        }
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
            if (!(level instanceof ServerLevel serverLevel)) {
                iterator.remove();
                continue;
            }

            int age = event.getServer().getTickCount() - strike.startTick;
            int currentTick = event.getServer().getTickCount();
            ServerPlayer shooter = strike.getShooter(event.getServer());
            if (age >= 700) {
                iterator.remove();
                damageEntities(serverLevel, strike, age, shooter, currentTick);
                explode(serverLevel, strike.key.pos(), shooter, currentTick);
            } else if (age >= 400 && OrbitalConfig.SUCK_ENTITIES.get()) {
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

    private static void damageEntities(ServerLevel level, ActiveStrike strike, int age, ServerPlayer shooter, int currentTick) {
        Vec3 center = Vec3.atCenterOf(strike.key.pos());
        Holder<DamageType> damageType = level.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(STRIKE_DAMAGE);
        DamageSource source = new DamageSource(damageType);
        double configuredDamage = OrbitalConfig.STRIKE_DAMAGE.get();
        if (configuredDamage <= 0.0D) {
            return;
        }
        float damage = (float) configuredDamage;
        boolean respectClaims = OrbitalConfig.RESPECT_CLAIMS.get();
        boolean allowEntityDamage = OrbitalConfig.ALLOW_ENTITY_DAMAGE_IN_CLAIMS.get();
        for (Entity entity : strike.entities) {
            if (entity == null || !entity.isAlive() || entity.level() != level) {
                continue;
            }
            if (entity.position().distanceToSqr(center) <= RADIUS_SQUARED) {
                if (respectClaims) {
                    boolean claimed = FTBChunksCompat.isPositionClaimed(level, entity.blockPosition());
                    if (claimed) {
                        if (!allowEntityDamage) {
                            notifyBlocked(shooter, currentTick);
                            continue;
                        }
                        if (!FTBChunksCompat.canDamageEntity(level, entity, shooter)) {
                            notifyBlocked(shooter, currentTick);
                            continue;
                        }
                    }
                }
                entity.hurt(source, damage);
            }
        }
    }

    private static void explode(ServerLevel level, BlockPos center, ServerPlayer shooter, int currentTick) {
        boolean respectClaims = OrbitalConfig.RESPECT_CLAIMS.get();
        boolean allowExplosions = OrbitalConfig.ALLOW_EXPLOSIONS_IN_CLAIMS.get();
        boolean allowBlockBreaks = OrbitalConfig.ALLOW_BLOCK_BREAK_IN_CLAIMS.get();
        if (respectClaims) {
            boolean claimedCenter = FTBChunksCompat.isPositionClaimed(level, center);
            if (claimedCenter) {
                if (!allowExplosions) {
                    notifyBlocked(shooter, currentTick);
                    return;
                }
                if (!FTBChunksCompat.canExplode(level, center, shooter)) {
                    notifyBlocked(shooter, currentTick);
                    return;
                }
            }
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (!MASK[x + RADIUS][z + RADIUS]) continue;

                    mutable.set(center.getX() + x, y, center.getZ() + z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.isAir()) continue;

                    double maxHardness = OrbitalConfig.MAX_BREAK_HARDNESS.get();
                    boolean infinite = maxHardness < 0.0D;

                    // Always resolve ID once
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());

                    // 1) Blacklist check ALWAYS applies
                    if (id != null && OrbitalConfig.isBlockBlacklistedNormalized(id.toString())) {
                        if (OrbitalConfig.DEBUG.get()) {
                            ForgeOrbitalRailgunMod.LOGGER.info("[OrbitalStrike] Skipped blacklisted block: {}", id);
                        }
                        continue;
                    }

                    // 2) Hardness check only applies when not infinite
                    if (!infinite) {
                        double hardness = state.getDestroySpeed(level, mutable);
                        if (hardness > maxHardness) {
                            if (OrbitalConfig.DEBUG.get()) {
                                ForgeOrbitalRailgunMod.LOGGER.info(
                                        "[OrbitalStrike] Skipped block due to hardness: {} ({} > {})",
                                        id, hardness, maxHardness
                                );
                            }
                            continue;
                        }
                    }

                    // Break the block
                    if (respectClaims) {
                        boolean claimed = FTBChunksCompat.isPositionClaimed(level, mutable);
                        if (claimed) {
                            if (!allowBlockBreaks) {
                                notifyBlocked(shooter, currentTick);
                                continue;
                            }
                            if (!FTBChunksCompat.canModifyBlock(level, mutable, shooter)) {
                                notifyBlocked(shooter, currentTick);
                                continue;
                            }
                        }
                    }

                    level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }


    private static void notifyBlocked(ServerPlayer shooter, int currentTick) {
        if (shooter == null) {
            return;
        }
        if (!FTBChunksCompat.isLoaded() || !OrbitalConfig.RESPECT_CLAIMS.get()) {
            return;
        }
        UUID id = shooter.getUUID();
        Integer lastTick = LAST_BLOCKED_MESSAGE_TICK.get(id);
        if (lastTick != null && lastTick == currentTick) {
            return;
        }
        shooter.displayClientMessage(Component.literal("❌ Railgun blocked by claim protection."), true);
        LAST_BLOCKED_MESSAGE_TICK.put(id, currentTick);
    }


    private record StrikeKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final class ActiveStrike {
        private final StrikeKey key;
        private final List<Entity> entities;
        private final int startTick;
        private final UUID shooter;

        private ActiveStrike(StrikeKey key, List<Entity> entities, int startTick, UUID shooter) {
            this.key = key;
            this.entities = entities;
            this.startTick = startTick;
            this.shooter = shooter;
        }

        private ServerPlayer getShooter(MinecraftServer server) {
            return server.getPlayerList().getPlayer(shooter);
        }
    }
}

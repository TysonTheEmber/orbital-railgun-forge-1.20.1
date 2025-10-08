package net.tysontheember.orbitalrailgun.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;
import net.tysontheember.orbitalrailgun.registry.ModSounds;
import net.tysontheember.orbitalrailgun.network.Network;
import net.tysontheember.orbitalrailgun.network.S2C_PlayStrikeEffects;
import net.tysontheember.orbitalrailgun.strike.StrikeExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.tysontheember.orbitalrailgun.compat.FTBChunksCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrbitalRailgunStrikeManager {
    private static final ResourceKey<DamageType> STRIKE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ForgeOrbitalRailgunMod.id("strike"));
    private static final Component CLAIM_BLOCKED_MESSAGE = Component.literal("❌ Railgun blocked by claim protection.");

    private static final Map<StrikeKey, ActiveStrike> ACTIVE_STRIKES = new ConcurrentHashMap<>();

    private OrbitalRailgunStrikeManager() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(OrbitalRailgunStrikeManager::onServerTick);
    }

    public static void startStrike(ServerPlayer player, BlockPos target) {
        ServerLevel serverLevel = player.serverLevel();
        if (serverLevel.isClientSide()) {
            return;
        }
        double configuredDiameter = OrbitalConfig.DESTRUCTION_DIAMETER.get();
        double clampedDiameter = Mth.clamp(configuredDiameter, 1.0D, 256.0D);
        double radius = clampedDiameter * 0.5D;

        double trackedExtent = Math.max(radius * 4.0D, 128.0D);
        List<Entity> tracked = new ArrayList<>(serverLevel.getEntities(null, AABB.ofSize(Vec3.atCenterOf(target), trackedExtent, trackedExtent, trackedExtent)));
        StrikeKey key = new StrikeKey(serverLevel.dimension(), target.immutable());
        ACTIVE_STRIKES.put(key, new ActiveStrike(key, tracked, player.getServer().getTickCount(), player.getUUID(), radius));

        if (ModSounds.RAILGUN_SHOOT.isPresent()) {
            serverLevel.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.RAILGUN_SHOOT.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        }
        float serverRadius = (float) radius;
        Network.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(target.getX(), target.getY(), target.getZ(), 512.0D, serverLevel.dimension())),
                new S2C_PlayStrikeEffects(target, serverLevel.dimension(), serverRadius)
        );
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Iterator<Map.Entry<StrikeKey, ActiveStrike>> iterator = ACTIVE_STRIKES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<StrikeKey, ActiveStrike> entry = iterator.next();
            ActiveStrike strike = entry.getValue();
            ServerLevel level = event.getServer().getLevel(strike.key.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            int age = event.getServer().getTickCount() - strike.startTick;
            if (age >= 700) {
                iterator.remove();
                damageEntities(level, strike, age);
                explode(level, strike);
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

    private static void damageEntities(ServerLevel level, ActiveStrike strike, int age) {
        // Center & radius
        Vec3 center = Vec3.atCenterOf(strike.key.pos());
        double radius = Math.sqrt(strike.radiusSquared);

        // Use the registry holder you already set up
        Holder<DamageType> damageType = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(STRIKE_DAMAGE);

        // Create the damage source (1.20.1 Mojang mappings support this ctor)
        DamageSource source = new DamageSource(damageType);

        // Bail if disabled
        double configuredDamage = OrbitalConfig.STRIKE_DAMAGE.get();
        if (configuredDamage <= 0.0D) return;
        float damage = (float) configuredDamage;

        // Respect claims (same flags you already use)
        boolean respectClaims = OrbitalConfig.RESPECT_CLAIMS.get() && FTBChunksCompat.isLoaded();
        boolean allowEntityDamage = OrbitalConfig.ALLOW_ENTITY_DAMAGE_IN_CLAIMS.get();
        ServerPlayer shooter = respectClaims ? resolveShooter(level, strike) : null;

        boolean blockedAny = false;
        BlockPos blockedPos = null;

        // **LIVE QUERY** of entities currently within radius (cylindrical-ish by AABB inflate)
        AABB box = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);
        List<Entity> targets = level.getEntities(null, box);

        for (Entity entity : targets) {
            if (entity == null || !entity.isAlive() || entity.level() != level) continue;
            if (entity instanceof Player p && p.isSpectator()) continue;

            // precise distance check (sphere)
            if (entity.position().distanceToSqr(center) > strike.radiusSquared) continue;

            if (respectClaims) {
                BlockPos entityPos = entity.blockPosition();

                if (FTBChunksCompat.isPositionClaimed(level, entityPos)) {
                    if (!allowEntityDamage) {
                        blockedAny = true;
                        blockedPos = entityPos.immutable();
                        continue;
                    }
                    if (shooter == null || !FTBChunksCompat.canDamageEntity(level, entity, shooter)) {
                        blockedAny = true;
                        blockedPos = entityPos.immutable();
                        continue;
                    }
                }
            }

            // Reset invulnerability frames a bit so the big blast actually lands
            entity.invulnerableTime = Math.min(entity.invulnerableTime, 2);

            // Apply damage
            entity.hurt(source, damage);
        }

        if (blockedAny) {
            notifyClaimBlocked(strike, shooter, ClaimBlockType.DAMAGE, blockedPos != null ? blockedPos : strike.key.pos());
        }
    }


    private static void explode(ServerLevel level, ActiveStrike strike) {
        BlockPos center = strike.key.pos();
        boolean respectClaims = OrbitalConfig.RESPECT_CLAIMS.get() && FTBChunksCompat.isLoaded();
        boolean allowExplosions = OrbitalConfig.ALLOW_EXPLOSIONS_IN_CLAIMS.get();
        boolean allowBlockBreak = OrbitalConfig.ALLOW_BLOCK_BREAK_IN_CLAIMS.get();
        ServerPlayer shooter = respectClaims ? resolveShooter(level, strike) : null;

        if (respectClaims && FTBChunksCompat.isPositionClaimed(level, center)) {
            if (!allowExplosions || shooter == null || !FTBChunksCompat.canExplode(level, center, shooter)) {
                notifyClaimBlocked(strike, shooter, ClaimBlockType.EXPLOSION, center);
                return;
            }
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean blockedAny = false;
        BlockPos blockedPos = null;
        int horizontalRange = strike.horizontalRange;

        // --- CHANGED: scan full vertical range from top build limit down to configured floor ---
        final int worldMinY = level.getMinBuildHeight();
        final int worldMaxY = level.getMaxBuildHeight() - 1;
        final int cfgFloor = OrbitalConfig.MIN_DESTROY_Y.get();
        final int yEnd   = Math.max(worldMinY, cfgFloor); // clamp to world min
        final int yStart = worldMaxY;                     // always start at top build limit

        LongOpenHashSet allowedPositions = new LongOpenHashSet();

        for (int y = yStart; y >= yEnd; --y) {
            for (int x = -horizontalRange; x <= horizontalRange; x++) {
                for (int z = -horizontalRange; z <= horizontalRange; z++) {
                    double horizontalDistanceSq = (double) x * x + (double) z * z;
                    if (horizontalDistanceSq > strike.radiusSquared) continue;

                    mutable.set(center.getX() + x, y, center.getZ() + z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.isAir()) continue;

                    if (respectClaims && FTBChunksCompat.isPositionClaimed(level, mutable)) {
                        if (!allowBlockBreak) {
                            blockedAny = true;
                            blockedPos = mutable.immutable();
                            continue;
                        }
                        if (shooter == null || !FTBChunksCompat.canModifyBlock(level, mutable, shooter)) {
                            blockedAny = true;
                            blockedPos = mutable.immutable();
                            continue;
                        }
                    }

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

                    allowedPositions.add(mutable.asLong());
                }
            }
        }
        if (blockedAny) {
            notifyClaimBlocked(strike, shooter, ClaimBlockType.BLOCKS, blockedPos != null ? blockedPos : center);
        }

        if (!allowedPositions.isEmpty()) {
            double radius = Math.sqrt(strike.radiusSquared);
            double diameter = radius * 2.0D;
            StrikeExecutor.begin(level, center, diameter);
            StrikeExecutor.filterAllowed(allowedPositions);
        }
    }

    private static ServerPlayer resolveShooter(ServerLevel level, ActiveStrike strike) {
        if (strike.shooter == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(strike.shooter);
    }

    private static void notifyClaimBlocked(ActiveStrike strike, ServerPlayer shooter, ClaimBlockType type, BlockPos pos) {
        if (strike.markNotified(type) && shooter != null) {
            shooter.displayClientMessage(CLAIM_BLOCKED_MESSAGE, true);
        }
        if (OrbitalConfig.DEBUG.get()) {
            ForgeOrbitalRailgunMod.LOGGER.info("[OrbitalStrike] Claim protection blocked {} at {}", type.name().toLowerCase(), pos);
        }
    }

    private record StrikeKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private enum ClaimBlockType {
        BLOCKS,
        EXPLOSION,
        DAMAGE
    }

    private static final class ActiveStrike {
        private final StrikeKey key;
        private final List<Entity> entities;
        private final int startTick;
        private final UUID shooter;
        private final double radiusSquared;
        private final int horizontalRange;
        private boolean blockNotified;
        private boolean explosionNotified;
        private boolean damageNotified;

        private ActiveStrike(StrikeKey key, List<Entity> entities, int startTick, UUID shooter, double radius) {
            this.key = key;
            this.entities = entities;
            this.startTick = startTick;
            this.shooter = shooter;
            this.radiusSquared = radius * radius;
            this.horizontalRange = Math.max(0, Mth.ceil(radius));
        }

        private boolean markNotified(ClaimBlockType type) {
            return switch (type) {
                case BLOCKS -> {
                    if (blockNotified) yield false;
                    blockNotified = true;
                    yield true;
                }
                case EXPLOSION -> {
                    if (explosionNotified) yield false;
                    explosionNotified = true;
                    yield true;
                }
                case DAMAGE -> {
                    if (damageNotified) yield false;
                    damageNotified = true;
                    yield true;
                }
            };
        }
    }
}

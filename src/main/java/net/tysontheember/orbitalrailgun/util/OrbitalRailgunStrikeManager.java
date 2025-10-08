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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrbitalRailgunStrikeManager {
    private static final int RADIUS = 24;
    private static final int RADIUS_SQUARED = RADIUS * RADIUS;
    private static final boolean[][] MASK = new boolean[RADIUS * 2 + 1][RADIUS * 2 + 1];
    private static final ResourceKey<DamageType> STRIKE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ForgeOrbitalRailgunMod.id("strike"));

    private static final Map<StrikeKey, ActiveStrike> ACTIVE_STRIKES = new ConcurrentHashMap<>();
    private static final Component CLAIM_BLOCKED_MESSAGE = Component.literal("❌ Railgun blocked by claim protection.");

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

    private static void damageEntities(Level level, ActiveStrike strike, int age) {
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
        boolean allowDamageInClaims = OrbitalConfig.ALLOW_ENTITY_DAMAGE_IN_CLAIMS.get();
        ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel) level : null;
        ServerPlayer shooter = null;
        if (respectClaims && serverLevel != null) {
            shooter = strike.resolveShooter(serverLevel);
        }

        for (Entity entity : strike.entities) {
            if (entity == null || !entity.isAlive() || entity.level() != level) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > RADIUS_SQUARED) {
                continue;
            }

            if (respectClaims && serverLevel != null) {
                boolean claimed = FTBChunksCompat.isClaimed(serverLevel, entity.blockPosition());
                if (claimed) {
                    boolean allowed = allowDamageInClaims && FTBChunksCompat.canDamageEntity(serverLevel, entity, shooter);
                    if (!allowed) {
                        strike.notifyBlocked(shooter, BlockedAction.ENTITIES, "entity damage denied by claim");
                        continue;
                    }
                }
            }

            entity.hurt(source, damage);
        }
    }

    private static void explode(Level level, ActiveStrike strike) {
        BlockPos center = strike.key.pos();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean respectClaims = OrbitalConfig.RESPECT_CLAIMS.get();
        boolean allowExplosionsInClaims = OrbitalConfig.ALLOW_EXPLOSIONS_IN_CLAIMS.get();
        boolean allowBlockBreakInClaims = OrbitalConfig.ALLOW_BLOCK_BREAK_IN_CLAIMS.get();
        ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel) level : null;
        ServerPlayer shooter = null;

        if (respectClaims && serverLevel != null) {
            shooter = strike.resolveShooter(serverLevel);
            if (FTBChunksCompat.isClaimed(serverLevel, center)) {
                if (!allowExplosionsInClaims) {
                    strike.notifyBlocked(shooter, BlockedAction.EXPLOSION, "explosion denied by claim");
                    return;
                }
                if (!FTBChunksCompat.canExplode(serverLevel, center, shooter)) {
                    strike.notifyBlocked(shooter, BlockedAction.EXPLOSION, "explosion permission denied");
                    return;
                }
            }
        }

        double maxHardness = OrbitalConfig.MAX_BREAK_HARDNESS.get();
        boolean infinite = maxHardness < 0.0D;

        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (!MASK[x + RADIUS][z + RADIUS]) {
                        continue;
                    }

                    mutable.set(center.getX() + x, y, center.getZ() + z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.isAir()) {
                        continue;
                    }

                    if (respectClaims && serverLevel != null && FTBChunksCompat.isClaimed(serverLevel, mutable)) {
                        if (!allowBlockBreakInClaims) {
                            strike.notifyBlocked(shooter, BlockedAction.BLOCKS, "block break denied by claim");
                            continue;
                        }
                        if (!FTBChunksCompat.canModifyBlock(serverLevel, mutable, shooter)) {
                            strike.notifyBlocked(shooter, BlockedAction.BLOCKS, "block break permission denied");
                            continue;
                        }
                    }

                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());

                    if (id != null && OrbitalConfig.isBlockBlacklistedNormalized(id.toString())) {
                        if (OrbitalConfig.DEBUG.get()) {
                            ForgeOrbitalRailgunMod.LOGGER.info("[OrbitalStrike] Skipped blacklisted block: {}", id);
                        }
                        continue;
                    }

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

                    level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }


    private record StrikeKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final class ActiveStrike {
        private final StrikeKey key;
        private final List<Entity> entities;
        private final int startTick;
        private final UUID shooterId;
        private final EnumSet<BlockedAction> notifiedActions = EnumSet.noneOf(BlockedAction.class);

        private ActiveStrike(StrikeKey key, List<Entity> entities, int startTick, UUID shooterId) {
            this.key = key;
            this.entities = entities;
            this.startTick = startTick;
            this.shooterId = shooterId;
        }

        private ServerPlayer resolveShooter(ServerLevel level) {
            if (shooterId == null) {
                return null;
            }
            return level.getServer().getPlayerList().getPlayer(shooterId);
        }

        private void notifyBlocked(ServerPlayer shooter, BlockedAction action, String reason) {
            boolean firstTime = notifiedActions.add(action);
            if (firstTime && shooter != null) {
                shooter.displayClientMessage(CLAIM_BLOCKED_MESSAGE, true);
            }
            if (OrbitalConfig.DEBUG.get()) {
                if (firstTime) {
                    ForgeOrbitalRailgunMod.LOGGER.info("[OrbitalStrike] Blocked {} due to {}", action, reason);
                } else {
                    ForgeOrbitalRailgunMod.LOGGER.debug("[OrbitalStrike] Additional block for {} due to {}", action, reason);
                }
            }
        }
    }

    private enum BlockedAction {
        BLOCKS,
        EXPLOSION,
        ENTITIES
    }
}

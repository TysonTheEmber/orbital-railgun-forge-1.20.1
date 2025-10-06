package net.tysontheember.orbitalrailgun.config;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OrbitalConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.DoubleValue MAX_BREAK_HARDNESS;
    public static final ForgeConfigSpec.IntValue COOLDOWN;
    public static final ForgeConfigSpec.IntValue STRIKE_DAMAGE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue suckEntities;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("orbitalStrike");
        MAX_BREAK_HARDNESS = builder
            .comment("Maximum block hardness the orbital strike can destroy(set to -1 for infinite)")
            .defineInRange("maxBreakHardness", 50.0D, -1.0D, Double.MAX_VALUE);
        COOLDOWN = builder
            .comment("Cooldown of the Railgun in ticks")
            .defineInRange("cooldown", 24000, 2000, Integer.MAX_VALUE);
        STRIKE_DAMAGE = builder
            .comment("Damage of the Orbital Strike")
            .defineInRange("strikeDamage", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        BLACKLISTED_BLOCKS = builder
            .comment("Blocks that cannot be destroyed by the orbital strike.")
            .defineList("blacklistedBlocks", List.of(
                "minecraft:bedrock",
                "minecraft:end_portal_frame",
                "minecraft:end_portal",
                "minecraft:barrier"
            ), o -> o instanceof String);
        suckEntities = builder
                .comment("Whether entities are pulled towards the strike before detonation.")
                .define("suckEntities", true);
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private OrbitalConfig() {}

    public static double getMaxBreakHardness() {
        return MAX_BREAK_HARDNESS.get();
    }

    public static List<? extends String> getBlacklistedBlocks() {
        return BLACKLISTED_BLOCKS.get();
    }

    public static boolean isBlockBlacklisted(String blockId) {
        return getBlacklistedBlocks().contains(blockId);
    }
}

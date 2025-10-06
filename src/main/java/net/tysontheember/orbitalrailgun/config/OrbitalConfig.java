package net.tysontheember.orbitalrailgun.config;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OrbitalConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.DoubleValue MAX_BREAK_HARDNESS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("orbitalStrike");
        MAX_BREAK_HARDNESS = builder
            .comment("Maximum block hardness the orbital strike can destroy.")
            .defineInRange("maxBreakHardness", 50.0D, 0.0D, Double.MAX_VALUE);
        BLACKLISTED_BLOCKS = builder
            .comment("Blocks that cannot be destroyed by the orbital strike.")
            .defineList("blacklistedBlocks", List.of(
                "minecraft:bedrock",
                "minecraft:end_portal_frame",
                "minecraft:end_portal",
                "minecraft:barrier"
            ), o -> o instanceof String);
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

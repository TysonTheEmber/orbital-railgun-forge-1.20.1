package net.tysontheember.orbitalrailgun.config;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OrbitalConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.DoubleValue MAX_BREAK_HARDNESS;
    public static final ForgeConfigSpec.IntValue COOLDOWN;
    public static final ForgeConfigSpec.IntValue STRIKE_DAMAGE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue SUCK_ENTITIES;
    public static final ForgeConfigSpec.DoubleValue DESTRUCTION_DIAMETER;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.BooleanValue RESPECT_CLAIMS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ENTITY_DAMAGE_IN_CLAIMS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_BLOCK_BREAK_IN_CLAIMS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_EXPLOSIONS_IN_CLAIMS;
    public static final ForgeConfigSpec.BooleanValue OPS_BYPASS_CLAIMS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("orbitalStrike");
        MAX_BREAK_HARDNESS = builder
            .comment("Maximum block hardness the orbital strike can destroy(set to -1 to destroy all blocks)")
            .defineInRange("maxBreakHardness", 50.0D, -1.0D, Double.MAX_VALUE);
        COOLDOWN = builder
            .comment("Cooldown of the Railgun in ticks")
            .defineInRange("cooldown", 2400, 1800, Integer.MAX_VALUE);
        STRIKE_DAMAGE = builder
            .comment("Damage of the Orbital Strike")
            .defineInRange("strikeDamage", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        BLACKLISTED_BLOCKS = builder
            .comment("Blocks that won't be destroyed by the orbital strike.")
            .defineList("blacklistedBlocks", List.of(
                "minecraft:bedrock",
                "minecraft:end_portal_frame",
                "minecraft:end_portal",
                "minecraft:barrier"
            ), o -> o instanceof String);
        SUCK_ENTITIES = builder
                .comment("Whether entities are pulled towards the strike before detonation.")
                .define("suckEntities", true);
        DESTRUCTION_DIAMETER = builder
                .comment("Diameter in blocks used for destruction/physics (server-authoritative).")
                .defineInRange("destructionDiameter", 12.0D, 1.0D, 256.0D);
        DEBUG = builder
                .comment("Toggle Debug mode")
                .define("debugMode", false);


        builder.pop();

        builder.push("protection");
        RESPECT_CLAIMS = builder
                .comment("If true, the railgun respects FTB Chunks claims and cancels actions in protected areas.")
                .define("respectClaims", true);
        ALLOW_ENTITY_DAMAGE_IN_CLAIMS = builder
                .comment("If true, the railgun can damage entities inside claims only if the shooter has permission.")
                .define("allowEntityDamageInClaims", false);
        ALLOW_BLOCK_BREAK_IN_CLAIMS = builder
                .comment("If true, the railgun can break blocks inside claims only if the shooter has permission.")
                .define("allowBlockBreakInClaims", false);
        ALLOW_EXPLOSIONS_IN_CLAIMS = builder
                .comment("If true, the railgun explosions are allowed inside claims only if the shooter has permission.")
                .define("allowExplosionsInClaims", false);
        OPS_BYPASS_CLAIMS = builder
                .comment("If true, operators (permission level >= 2) bypass claim checks.")
                .define("opsBypassClaims", true);
        builder.pop();

        COMMON_SPEC = builder.build();

    }

    private OrbitalConfig() {}

    public static boolean isBlockBlacklistedNormalized(String blockId) {
        if (blockId == null) return false;
        String target = blockId.trim().toLowerCase();
        for (String s : BLACKLISTED_BLOCKS.get()) {
            if (s != null && target.equals(s.trim().toLowerCase())) return true;
        }
        return false;
    }
}

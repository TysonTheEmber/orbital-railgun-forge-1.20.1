package net.tysontheember.orbitalrailgun.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class OrbitalRailgunConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Common COMMON;
    public static final Client CLIENT;

    static {
        Pair<Common, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = commonPair.getRight();
        COMMON = commonPair.getLeft();

        Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    private OrbitalRailgunConfig() {}

    public static final class Common {
        public final ForgeConfigSpec.DoubleValue strikeDamage;
        public final ForgeConfigSpec.BooleanValue breakBedrock;
        public final ForgeConfigSpec.BooleanValue suckEntities;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("orbital_railgun");
            strikeDamage = builder
                .comment("Damage dealt by the orbital strike to entities within its radius.")
                .defineInRange("strikeDamage", 100000.0D, 0.0D, Double.MAX_VALUE);
            breakBedrock = builder
                .comment("Whether the orbital strike breaks bedrock within its blast radius.")
                .define("breakBedrock", true);
            suckEntities = builder
                .comment("Whether entities are pulled towards the strike before detonation.")
                .define("suckEntities", true);
            builder.pop();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue disableWithShaderpack;
        public final ForgeConfigSpec.BooleanValue logIrisState;
        public final ForgeConfigSpec.BooleanValue forceVanillaPostChain;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            disableWithShaderpack = builder
                .comment("Disable the orbital railgun overlays when a shader pack is active.")
                .define("disableWithShaderpack", false);
            logIrisState = builder
                .comment("Log shader pack compatibility decisions whenever resources reload.")
                .define("logIrisState", true);
            forceVanillaPostChain = builder
                .comment("Force the vanilla post-processing chain to run even when a shader pack is detected.")
                .define("forceVanillaPostChain", false);
            builder.pop();
        }
    }
}

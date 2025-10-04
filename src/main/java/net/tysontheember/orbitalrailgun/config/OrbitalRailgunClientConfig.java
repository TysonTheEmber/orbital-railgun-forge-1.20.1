package net.tysontheember.orbitalrailgun.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class OrbitalRailgunClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = pair.getRight();
        CLIENT = pair.getLeft();
    }

    private OrbitalRailgunClientConfig() {}

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue compatDisableWithShaderpack;
        public final ForgeConfigSpec.BooleanValue compatLogIrisState;
        public final ForgeConfigSpec.BooleanValue compatForceVanillaPostChain;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            compatDisableWithShaderpack = builder
                .comment("Disable the vanilla orbital railgun PostChain when an Iris shader-pack is active.")
                .define("disableWithShaderpack", true);
            compatLogIrisState = builder
                .comment("Log when Iris shader-packs are detected and the vanilla PostChain is disabled.")
                .define("logIrisState", false);
            compatForceVanillaPostChain = builder
                .comment("Attempt to use the vanilla PostChain regardless of other compat options. Ignored when Iris is present.")
                .define("forceVanillaPostChain", false);
            builder.pop();
        }
    }
}

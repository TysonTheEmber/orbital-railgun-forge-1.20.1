package net.tysontheember.orbitalrailgun.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class OrbitalRailgunClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    private OrbitalRailgunClientConfig() {}

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue disableWithShaderpack;
        public final ForgeConfigSpec.BooleanValue logIrisState;
        public final ForgeConfigSpec.BooleanValue forceVanillaPostChain;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            disableWithShaderpack = builder
                .comment("Disable orbital railgun post effects entirely when a shader pack is active.")
                .define("disableWithShaderpack", false);
            logIrisState = builder
                .comment("Log whether Iris/Oculus shader packs are detected during reloads.")
                .define("logIrisState", true);
            forceVanillaPostChain = builder
                .comment("Always run the vanilla post-processing chain even when a shader pack is active.")
                .define("forceVanillaPostChain", false);
            builder.pop();
        }
    }
}

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

    private OrbitalRailgunClientConfig() {
    }

    public static final class Client {
        public final Compat compat;

        private Client(ForgeConfigSpec.Builder builder) {
            compat = new Compat(builder);
        }
    }

    public static final class Compat {
        public final ForgeConfigSpec.BooleanValue disableWithShaderpack;
        public final ForgeConfigSpec.BooleanValue logIrisState;
        public final ForgeConfigSpec.BooleanValue forceVanillaPostChain;

        private Compat(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            disableWithShaderpack = builder
                .comment("If true, the orbital railgun post-processing is fully disabled while a shader pack is active.")
                .define("disableWithShaderpack", false);
            logIrisState = builder
                .comment("If true, logs whether an Iris/Oculus shader pack is detected when resources reload.")
                .define("logIrisState", true);
            forceVanillaPostChain = builder
                .comment("If true, always use the vanilla post-processing chain even when a shader pack is active.")
                .define("forceVanillaPostChain", false);
            builder.pop();
        }
    }
}

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
        public final ForgeConfigSpec.BooleanValue useWorldspaceAndHUD;
        public final ForgeConfigSpec.BooleanValue allowVanillaPostChain;
        public final ForgeConfigSpec.BooleanValue logIrisState;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            useWorldspaceAndHUD = builder
                .comment("Render orbital railgun effects using the world-space and HUD renderer when available.")
                .define("useWorldspaceAndHUD", true);
            allowVanillaPostChain = builder
                .comment("Allow the legacy vanilla post-processing chain to run when shaderpacks are inactive.")
                .define("allowVanillaPostChain", false);
            logIrisState = builder
                .comment("Log Iris/Oculus shaderpack detection information for debugging.")
                .define("logIrisState", true);
            builder.pop();
        }
    }
}

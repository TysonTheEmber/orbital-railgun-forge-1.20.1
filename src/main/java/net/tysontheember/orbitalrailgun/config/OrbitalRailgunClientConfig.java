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

    private OrbitalRailgunClientConfig() {
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue useWorldspaceAndHud;
        public final ForgeConfigSpec.BooleanValue allowVanillaPostChain;
        public final ForgeConfigSpec.BooleanValue logIrisState;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            useWorldspaceAndHud = builder
                .comment("If true, render orbital railgun FX using world-space geometry and HUD overlays.")
                .define("useWorldspaceAndHUD", true);
            allowVanillaPostChain = builder
                .comment("Allows the legacy vanilla PostChain renderer to run when no shaderpack is active.")
                .define("allowVanillaPostChain", false);
            logIrisState = builder
                .comment("Log the detected Iris/Oculus shaderpack state when first queried.")
                .define("logIrisState", true);
            builder.pop();
        }
    }
}

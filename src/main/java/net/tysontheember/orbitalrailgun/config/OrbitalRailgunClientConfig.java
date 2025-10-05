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
        public final ForgeConfigSpec.BooleanValue useWorldspaceAndHud;
        public final ForgeConfigSpec.BooleanValue allowVanillaPostChain;
        public final ForgeConfigSpec.BooleanValue logIrisState;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("compat");
            useWorldspaceAndHud = builder
                .comment("Render orbital effects via world-space and HUD quads when shaderpacks are active.")
                .define("useWorldspaceAndHUD", true);
            allowVanillaPostChain = builder
                .comment("Allow the vanilla post chain fallback when no shaderpack is active.")
                .define("allowVanillaPostChain", false);
            logIrisState = builder
                .comment("Log Iris/Oculus shaderpack detection for debugging.")
                .define("logIrisState", true);
            builder.pop();
        }
    }
}

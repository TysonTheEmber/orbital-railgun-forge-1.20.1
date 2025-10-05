package net.tysontheember.orbitalrailgun.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue COMPAT_OVERLAY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue COMPAT_FORCE_VANILLA_POSTCHAIN;
    public static final ForgeConfigSpec.BooleanValue COMPAT_LOG_IRIS_STATE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("compat");
        COMPAT_OVERLAY_ENABLED = builder
                .comment("Enable lightweight HUD overlay in shader-pack mode")
                .define("overlayEnabled", true);
        COMPAT_FORCE_VANILLA_POSTCHAIN = builder
                .comment("Force vanilla PostChain even if a shader pack is detected (not recommended)")
                .define("forceVanillaPostChain", false);
        COMPAT_LOG_IRIS_STATE = builder
                .comment("Log Iris/Oculus shader-pack detection state at startup/reload")
                .define("logIrisState", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}

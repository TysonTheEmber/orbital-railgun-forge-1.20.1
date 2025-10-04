package net.tysontheember.orbitalrailgun.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue COMPAT_DISABLE_WITH_SHADERPACK;
    public static final ForgeConfigSpec.BooleanValue COMPAT_OVERLAY_WITH_SHADERPACK;
    public static final ForgeConfigSpec.BooleanValue COMPAT_FORCE_VANILLA_POSTCHAIN;
    public static final ForgeConfigSpec.BooleanValue COMPAT_LOG_IRIS_STATE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("compat");
        COMPAT_DISABLE_WITH_SHADERPACK = builder
                .comment("Skip PostChain when Oculus/Iris shaderpack is active.")
                .define("disableWithShaderpack", true);
        COMPAT_OVERLAY_WITH_SHADERPACK = builder
                .comment("Render a GUI overlay effect when shaderpack compatibility mode is active.")
                .define("overlayWithShaderpack", true);
        COMPAT_FORCE_VANILLA_POSTCHAIN = builder
                .comment("Force PostChain even with shaderpack (debug).")
                .define("forceVanillaPostChain", false);
        COMPAT_LOG_IRIS_STATE = builder
                .comment("Log shaderpack detection once per reload/world join.")
                .define("logIrisState", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}

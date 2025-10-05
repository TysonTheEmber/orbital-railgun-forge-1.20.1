package net.tysontheember.orbitalrailgun.client.fx;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.config.OrbitalRailgunClientConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;

public final class IrisCompat {
    private static final String IRIS_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String OCULUS_API_CLASS = "net.irisshaders.oculus.api.v0.OculusApi";

    private static boolean loggedState;
    private static boolean lastState;

    private IrisCompat() {}

    public static boolean isShaderpackPresent() {
        return ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
    }

    public static boolean isShaderpackActive() {
        if (!FMLEnvironment.dist.isClient()) {
            return false;
        }

        boolean active = queryShaderpackState();
        if (OrbitalRailgunClientConfig.CLIENT.logIrisState.get()) {
            if (!loggedState || active != lastState) {
                ForgeOrbitalRailgunMod.LOGGER.info("Iris/Oculus shaderpack active: {}", active);
                loggedState = true;
                lastState = active;
            }
        }
        return active;
    }

    private static boolean queryShaderpackState() {
        Method isShaderPackInUse = null;
        Object apiInstance = null;

        String className = null;
        if (ModList.get().isLoaded("iris")) {
            className = IRIS_API_CLASS;
        } else if (ModList.get().isLoaded("oculus")) {
            className = OCULUS_API_CLASS;
        }

        if (className == null) {
            return false;
        }

        try {
            Class<?> apiClass = Class.forName(className);
            Method getInstance = apiClass.getMethod("getInstance");
            apiInstance = getInstance.invoke(null);
            isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
        } catch (Exception exception) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Failed to query Iris/Oculus API", exception);
            return false;
        }

        if (apiInstance == null || isShaderPackInUse == null) {
            return false;
        }

        try {
            Object result = isShaderPackInUse.invoke(apiInstance);
            return result instanceof Boolean bool && bool;
        } catch (Exception exception) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Failed to invoke isShaderPackInUse", exception);
            return false;
        }
    }
}

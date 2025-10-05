package net.tysontheember.orbitalrailgun.client.fx;

import net.minecraftforge.fml.ModList;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.lang.reflect.Method;

public final class IrisCompat {
    private static final String IRIS_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static boolean checked;
    private static boolean irisPresent;
    private static Method isShaderPackInUseMethod;
    private static Object irisInstance;

    private IrisCompat() {
    }

    public static boolean isShaderpackActive() {
        ensureLookup();
        if (!irisPresent || isShaderPackInUseMethod == null || irisInstance == null) {
            return false;
        }
        try {
            return (boolean) isShaderPackInUseMethod.invoke(irisInstance);
        } catch (Throwable throwable) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Failed to query Iris shader pack state", throwable);
        }
        return false;
    }

    private static void ensureLookup() {
        if (checked) {
            return;
        }
        checked = true;

        if (!isIrisModLoaded()) {
            irisPresent = false;
            return;
        }

        try {
            Class<?> irisApi = Class.forName(IRIS_API_CLASS);
            Method getInstance = irisApi.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            if (instance == null) {
                irisPresent = false;
                return;
            }
            Method handle = irisApi.getMethod("isShaderPackInUse");
            irisInstance = instance;
            isShaderPackInUseMethod = handle;
            irisPresent = true;
        } catch (Throwable throwable) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Unable to initialise Iris compatibility", throwable);
            irisPresent = false;
        }
    }

    private static boolean isIrisModLoaded() {
        return ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
    }
}

package net.tysontheember.orbitalrailgun.client;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ShaderModBridge {
    private static final boolean IRIS_LOADED = ModList.get().isLoaded("iris");
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("oculus");
    private static final boolean SHADER_MOD_PRESENT = IRIS_LOADED || OCULUS_LOADED;

    private static boolean reflectionInitialized;
    private static Method shaderPackInUseMethod;
    private static Object irisApiInstance;

    private ShaderModBridge() {}

    public static boolean isShaderModPresent() {
        return SHADER_MOD_PRESENT;
    }

    public static boolean isShaderPackInUse() {
        if (!SHADER_MOD_PRESENT) {
            return false;
        }
        ensureReflection();
        if (shaderPackInUseMethod == null || irisApiInstance == null) {
            return false;
        }
        try {
            Object result = shaderPackInUseMethod.invoke(irisApiInstance);
            return result instanceof Boolean bool && bool;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Failed to query Iris shader pack state", exception);
            return false;
        }
    }

    public static void reset() {
        reflectionInitialized = false;
        shaderPackInUseMethod = null;
        irisApiInstance = null;
    }

    private static void ensureReflection() {
        if (reflectionInitialized) {
            return;
        }
        reflectionInitialized = true;
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = irisApiClass.getMethod("getInstance");
            Method isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            Object instance = getInstance.invoke(null);
            if (instance != null) {
                irisApiInstance = instance;
                shaderPackInUseMethod = isShaderPackInUse;
            }
        } catch (ReflectiveOperationException exception) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Iris API unavailable while checking shader pack state", exception);
        }
    }
}

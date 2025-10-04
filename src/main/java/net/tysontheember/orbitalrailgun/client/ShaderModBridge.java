package net.tysontheember.orbitalrailgun.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public final class ShaderModBridge {
    private static final String IRIS_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";

    private static final boolean IRIS_OR_OCULUS_LOADED = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");

    private static boolean attemptedInit;
    private static Object irisApiInstance;
    private static Method shaderPackInUseMethod;

    private ShaderModBridge() {}

    public static boolean isShaderModPresent() {
        return IRIS_OR_OCULUS_LOADED;
    }

    public static boolean isShaderPackInUse() {
        if (!isShaderModPresent()) {
            return false;
        }
        ensureIrisApi();
        if (irisApiInstance != null && shaderPackInUseMethod != null) {
            try {
                Object result = shaderPackInUseMethod.invoke(irisApiInstance);
                if (result instanceof Boolean bool) {
                    return bool;
                }
            } catch (IllegalAccessException | InvocationTargetException exception) {
                ForgeOrbitalRailgunMod.LOGGER.warn("Failed to query Iris shader pack state", exception);
                irisApiInstance = null;
                shaderPackInUseMethod = null;
                attemptedInit = false;
            }
        }
        return false;
    }

    private static void ensureIrisApi() {
        if (attemptedInit || !isShaderModPresent()) {
            return;
        }
        attemptedInit = true;
        try {
            Class<?> irisApiClass = Class.forName(IRIS_API_CLASS);
            Method getInstance = irisApiClass.getMethod("getInstance");
            Method isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            Object instance = getInstance.invoke(null);
            if (instance != null) {
                irisApiInstance = instance;
                shaderPackInUseMethod = isShaderPackInUse;
            }
        } catch (ClassNotFoundException ignored) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Iris API class was not found while attempting to initialize shader bridge");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            ForgeOrbitalRailgunMod.LOGGER.warn("Unable to initialize Iris shader bridge", exception);
        }
    }
}

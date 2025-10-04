package net.tysontheember.orbitalrailgun.client;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ShaderModBridge {
    private static final boolean IRIS_LOADED = ModList.get().isLoaded("iris");
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("oculus");

    private static final Method IRIS_GET_INSTANCE;
    private static final Method IRIS_IS_SHADER_PACK_IN_USE;

    static {
        Method getInstance = null;
        Method isShaderPackInUse = null;
        if (isShaderModPresent()) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                getInstance = irisApiClass.getMethod("getInstance");
                isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                ForgeOrbitalRailgunMod.LOGGER.debug("Unable to resolve Iris API for shader detection", exception);
                getInstance = null;
                isShaderPackInUse = null;
            }
        }
        IRIS_GET_INSTANCE = getInstance;
        IRIS_IS_SHADER_PACK_IN_USE = isShaderPackInUse;
    }

    private ShaderModBridge() {}

    public static boolean isShaderModPresent() {
        return IRIS_LOADED || OCULUS_LOADED;
    }

    public static boolean isShaderPackInUse() {
        if (!isShaderModPresent()) {
            return false;
        }
        if (IRIS_GET_INSTANCE == null || IRIS_IS_SHADER_PACK_IN_USE == null) {
            return false;
        }
        try {
            Object instance = IRIS_GET_INSTANCE.invoke(null);
            if (instance instanceof Optional<?> optional) {
                instance = optional.orElse(null);
            }
            if (instance == null) {
                return false;
            }
            Object value = IRIS_IS_SHADER_PACK_IN_USE.invoke(instance);
            return value instanceof Boolean bool ? bool : false;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Failed to query Iris shader pack state", exception);
            return false;
        }
    }
}

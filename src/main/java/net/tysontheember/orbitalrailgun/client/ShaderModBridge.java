package net.tysontheember.orbitalrailgun.client;

import net.minecraftforge.fml.ModList;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ShaderModBridge {
    private static final boolean IRIS_LOADED = ModList.get().isLoaded("iris");
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("oculus");

    private static final MethodHandle IRIS_GET_INSTANCE;
    private static final MethodHandle IRIS_IS_SHADER_PACK_IN_USE;

    static {
        MethodHandle getInstance = null;
        MethodHandle isShaderPackInUse = null;

        if (IRIS_LOADED) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                getInstance = lookup.findStatic(irisApiClass, "getInstance", MethodType.methodType(irisApiClass));
                isShaderPackInUse = lookup.findVirtual(irisApiClass, "isShaderPackInUse", MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException exception) {
                ForgeOrbitalRailgunMod.LOGGER.warn("Failed to initialise Iris reflection bridge", exception);
                getInstance = null;
                isShaderPackInUse = null;
            }
        }

        IRIS_GET_INSTANCE = getInstance;
        IRIS_IS_SHADER_PACK_IN_USE = isShaderPackInUse;
    }

    private ShaderModBridge() {
    }

    public static boolean isShaderPackInUse() {
        if (!IRIS_LOADED && !OCULUS_LOADED) {
            return false;
        }
        if (IRIS_GET_INSTANCE == null || IRIS_IS_SHADER_PACK_IN_USE == null) {
            return false;
        }
        try {
            Object instance = IRIS_GET_INSTANCE.invoke();
            if (instance == null) {
                return false;
            }
            return (boolean) IRIS_IS_SHADER_PACK_IN_USE.invoke(instance);
        } catch (Throwable throwable) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Unable to query Iris shader state", throwable);
            return false;
        }
    }

    public static boolean isShaderModPresent() {
        return IRIS_LOADED || OCULUS_LOADED;
    }
}

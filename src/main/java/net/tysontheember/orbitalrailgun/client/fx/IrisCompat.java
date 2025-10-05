package net.tysontheember.orbitalrailgun.client.fx;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.config.OrbitalRailgunClientConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public final class IrisCompat {
    private static final boolean IRIS_AVAILABLE = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
    private static final Method API_GET_INSTANCE;
    private static final Method API_IS_ACTIVE;
    private static final Method API_GET_PACK_NAME;

    private static Object irisApiInstance;
    private static boolean loggedState;

    static {
        Method getInstance = null;
        Method isActive = null;
        Method getPack = null;
        if (IRIS_AVAILABLE) {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                getInstance = apiClass.getMethod("getInstance");
                isActive = apiClass.getMethod("isShaderPackInUse");
                getPack = apiClass.getMethod("getActiveShaderPackName");
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                ForgeOrbitalRailgunMod.LOGGER.warn("Failed to locate Iris API", exception);
            }
        }
        API_GET_INSTANCE = getInstance;
        API_IS_ACTIVE = isActive;
        API_GET_PACK_NAME = getPack;
    }

    private IrisCompat() {
    }

    public static boolean isShaderpackActive() {
        if (!IRIS_AVAILABLE || API_GET_INSTANCE == null || API_IS_ACTIVE == null) {
            return false;
        }
        try {
            if (irisApiInstance == null) {
                irisApiInstance = API_GET_INSTANCE.invoke(null);
            }
            if (irisApiInstance == null) {
                return false;
            }
            boolean active = (boolean) API_IS_ACTIVE.invoke(irisApiInstance);
            if (OrbitalRailgunClientConfig.CLIENT.logIrisState.get() && !loggedState) {
                loggedState = true;
                String packName = "<none>";
                if (API_GET_PACK_NAME != null) {
                    Object value = API_GET_PACK_NAME.invoke(irisApiInstance);
                    if (value instanceof String string && !string.isEmpty()) {
                        packName = string;
                    }
                }
                ForgeOrbitalRailgunMod.LOGGER.info("Iris/Oculus shaderpack active: {} (pack={})", active, packName);
            }
            return active;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            ForgeOrbitalRailgunMod.LOGGER.warn("Failed to query Iris shaderpack state", exception);
            return false;
        }
    }

    public static String getActiveShaderpack() {
        if (!IRIS_AVAILABLE || API_GET_INSTANCE == null || API_GET_PACK_NAME == null) {
            return "";
        }
        try {
            if (irisApiInstance == null) {
                irisApiInstance = API_GET_INSTANCE.invoke(null);
            }
            if (irisApiInstance == null) {
                return "";
            }
            Object value = API_GET_PACK_NAME.invoke(irisApiInstance);
            return value instanceof String string ? string : "";
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return "";
        }
    }
}

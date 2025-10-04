package net.tysontheember.orbitalrailgun.client.compat;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class OculusCompat {
    private static boolean checked;
    private static boolean active;

    private OculusCompat() {}

    /** Refresh detection once per client tick. */
    public static void tick() {
        checked = false;
    }

    public static boolean isShaderpackActive() {
        if (!checked) {
            checked = true;
            active = detect();
        }
        return active;
    }

    private static boolean detect() {
        boolean hasOculus = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
        if (!hasOculus) {
            return false;
        }
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = api.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method isInUse = api.getMethod("isShaderPackInUse");
            Object result = isInUse.invoke(instance);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

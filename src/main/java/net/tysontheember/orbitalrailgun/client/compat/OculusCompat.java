package net.tysontheember.orbitalrailgun.client.compat;

import net.minecraftforge.fml.ModList;

public final class OculusCompat {
    private static boolean checked;
    private static boolean active;

    /** refresh detection cheaply once per client tick */
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
        if (!(ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris"))) {
            return false;
        }
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object result = api.getMethod("isShaderPackInUse").invoke(instance);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private OculusCompat() {}
}

package net.tysontheember.orbitalrailgun.client;

import net.minecraftforge.fml.ModList;

public final class IrisCompat {
    private static Boolean cached;

    private IrisCompat() {}

    public static boolean isActive() {
        if (cached != null) return cached;

        // Quick mod presence check to avoid reflection cost if not installed
        if (!ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) {
            return cached = false;
        }

        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            boolean active = (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
            return cached = active;
        } catch (Throwable t) {
            return cached = false;
        }
    }

    public static void clearOnReload() {
        cached = null;
    }
}

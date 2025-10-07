package net.tysontheember.orbitalrailgun.client.compat;

import net.minecraftforge.fml.ModList;

public final class IrisCompat {
    private static boolean lastKnown = false;
    private static boolean haveIris = false;
    private static boolean initialized = false;

    private IrisCompat() {}

    public static void bootstrap() {
        if (initialized) return;
        initialized = true;
        haveIris = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
        // Prime the cache
        lastKnown = queryIrisActive();
    }

    public static boolean isShaderpackActive() {
        if (!haveIris) return false;
        boolean now = queryIrisActive();
        lastKnown = now;
        return now;
    }

    public static boolean pollStateChanged() {
        if (!haveIris) return false;
        boolean now = queryIrisActive();
        boolean changed = (now != lastKnown);
        lastKnown = now;
        return changed;
    }

    private static boolean queryIrisActive() {
        if (!haveIris) return false;
        try {
            // net.irisshaders.iris.api.v0.IrisApi#isShaderPackInUse()
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Boolean inUse = (Boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
            return inUse != null && inUse;
        } catch (Throwable t) {
            // If API missing or fails, assume not active to avoid accidental hard-disable.
            return false;
        }
    }
}

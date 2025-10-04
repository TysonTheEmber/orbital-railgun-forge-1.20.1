package net.tysontheember.orbitalrailgun.client.compat;

/**
 * Minimal runtime Iris/Oculus integration that relies on reflection in order to
 * avoid a hard dependency on the API jar. The methods are intentionally kept
 * defensive so the mod behaves normally when Iris is not present.
 */
public final class IrisCompat {
    private static Boolean cached;

    private IrisCompat() {}

    /**
     * @return {@code true} when an Iris/Oculus shader pack is currently active.
     */
    public static boolean shaderpackEnabled() {
        Boolean cachedValue = cached;
        if (cachedValue != null) {
            return cachedValue;
        }

        boolean result = false;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            result = (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Throwable ignored) {
            result = false;
        }

        cached = result;
        return result;
    }

    /**
     * Clears the cached shader-pack state forcing the next query to ask Iris
     * again. Useful when resource reloads occur after toggling shader packs.
     */
    public static void invalidateCache() {
        cached = null;
    }
}

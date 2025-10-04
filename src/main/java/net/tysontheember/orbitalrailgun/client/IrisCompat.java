package net.tysontheember.orbitalrailgun.client;

public final class IrisCompat {
    private static Boolean cached;

    public static boolean isActive() {
        if (cached != null) return cached;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            boolean active = (boolean) api.getMethod("isShaderPackInUse").invoke(inst);
            cached = active;
            return active;
        } catch (Throwable t) {
            cached = false;
            return false;
        }
    }

    public static void clearOnReload() { cached = null; }
}

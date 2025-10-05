package net.tysontheember.orbitalrailgun.client.fx;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

public final class IrisCompat {
    private static final boolean PRESENT;

    static {
        boolean present;
        try {
            Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            present = true;
        } catch (ClassNotFoundException exception) {
            present = false;
        }
        PRESENT = present;
    }

    private IrisCompat() {
    }

    public static boolean isShaderpackActive() {
        if (!PRESENT) {
            return false;
        }
        try {
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            if (api == null) {
                return false;
            }
            Object active = api.getClass().getMethod("isShaderPackInUse").invoke(api);
            return active instanceof Boolean && (Boolean) active;
        } catch (ReflectiveOperationException exception) {
            ForgeOrbitalRailgunMod.LOGGER.debug("Iris API not accessible", exception);
            return false;
        }
    }
}

package net.tysontheember.orbitalrailgun.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;

public final class ShaderPackAddonDetector {
    private static final Logger LOG = LogManager.getLogger("orbital_railgun");

    private static volatile boolean cacheValid = false;
    private static volatile boolean addonActive = false;
    private static volatile boolean anyPackInUse = false;
    private static volatile String  packName = "";

    private ShaderPackAddonDetector(){}

    public static void hookReloadInvalidation() {
        if (Minecraft.getInstance().getResourceManager() instanceof ReloadableResourceManager rrm) {
            rrm.registerReloadListener((barrier, manager, p1, p2, bg, game) ->
                    barrier.wait(false).thenRunAsync(ShaderPackAddonDetector::invalidate, game));
        }
    }

    public static void invalidate() { cacheValid = false; }

    public static boolean isAnyPackInUse() { ensure(); return anyPackInUse; }

    public static boolean isAddonActive() { ensure(); return addonActive; }

    public static String currentPackName() { ensure(); return packName; }

    private static boolean nameMatchesAddon(String raw) {
        if (raw == null) return false;
        String n = raw.replace('\\','/');
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);
        if (n.toLowerCase(Locale.ROOT).endsWith(".zip")) n = n.substring(0, n.length() - 4);
        n = n.trim().toLowerCase(Locale.ROOT);
        return n.equals("orbitalrailgun-addon") || n.equals("orbital_railgun-addon");
    }

    private static void ensure() {
        if (cacheValid) return;

        boolean inUse = false;
        String name = "";

        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = irisApi.getMethod("getInstance").invoke(null);
            inUse = (Boolean) irisApi.getMethod("isShaderPackInUse").invoke(api);
            name  = Objects.toString(irisApi.getMethod("getCurrentPackName").invoke(api), "");
        } catch (Throwable ignored) {
            try {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                name  = Objects.toString(iris.getMethod("getCurrentShaderPackName").invoke(null), "");
                inUse = !name.isBlank() && !"OFF".equalsIgnoreCase(name);
            } catch (Throwable ignored2) {
                inUse = false;
                name = "";
            }
        }

        packName = name;
        anyPackInUse = inUse;
        addonActive = inUse && nameMatchesAddon(name);
        cacheValid = true;

        LOG.info("[orbital_railgun] Iris packInUse={} packName='{}' addonActive={}", inUse, name, addonActive);
    }
}

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

    // Accept folder or zip; ignore path and case
    private static boolean nameMatchesAddon(String raw) {
        if (raw == null) return false;
        String n = raw.replace('\\','/');               // normalize slashes
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);     // strip path
        if (n.toLowerCase(Locale.ROOT).endsWith(".zip")) n = n.substring(0, n.length() - 4); // strip .zip
        n = n.trim().toLowerCase(Locale.ROOT);
        // allow hyphen/underscore variants
        return n.equals("orbitalrailgun-addon") || n.equals("orbital_railgun-addon");
    }

    /** Call once during client init to tie into F3+T reloads. */
    public static void hookReloadInvalidation() {
        if (Minecraft.getInstance().getResourceManager() instanceof ReloadableResourceManager rrm) {
            rrm.registerReloadListener((barrier, manager, prepProfiler, applyProfiler, bg, game) ->
                    barrier.wait(false).thenRunAsync(ShaderPackAddonDetector::invalidate, game));
        }
    }

    public static void invalidate() { cacheValid = false; }

    public static boolean isAnyPackInUse() { ensure(); return anyPackInUse; }

    public static boolean isAddonActive() { ensure(); return addonActive; }

    public static String currentPackName() { ensure(); return packName; }

    private static void ensure() {
        if (cacheValid) return;

        boolean inUse = false;
        String name = "";

        // Try Iris API v0 (present in Oculus)
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = irisApi.getMethod("getInstance").invoke(null);
            inUse = (Boolean) irisApi.getMethod("isShaderPackInUse").invoke(api);
            name  = Objects.toString(irisApi.getMethod("getCurrentPackName").invoke(api), "");
        } catch (Throwable ignored) {
            // Fallback: static helper in some builds
            try {
                Class<?> irisCls = Class.forName("net.irisshaders.iris.Iris");
                name  = Objects.toString(irisCls.getMethod("getCurrentShaderPackName").invoke(null), "");
                inUse = !name.isBlank() && !"OFF".equalsIgnoreCase(name);
            } catch (Throwable ignored2) {
                inUse = false; name = "";
            }
        }

        packName = name;
        anyPackInUse = inUse;
        addonActive = inUse && nameMatchesAddon(name);
        cacheValid = true;

        LOG.info("[orbital_railgun] Iris packInUse={} packName='{}' addonActive={}", inUse, name, addonActive);
    }
}

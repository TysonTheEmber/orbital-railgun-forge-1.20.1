package net.tysontheember.orbitalrailgun.client.compat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Method;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class ShaderPackAddonDetector {
    private static boolean cached;
    private static boolean value;

    private ShaderPackAddonDetector() {
    }

    public static boolean isAddonActive() {
        if (!cached) {
            value = detectAddon();
            cached = true;
        }
        return value;
    }

    public static void invalidate() {
        cached = false;
    }

    private static boolean detectAddon() {
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, ShaderPackAddonDetector.class.getClassLoader());
            Method getInstance = irisApiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            try {
                Method getCurrentShaderPack = irisApiClass.getMethod("getCurrentShaderPack");
                Object pack = getCurrentShaderPack.invoke(api);
                if (pack != null && containsAddonName(pack.toString())) {
                    return true;
                }
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Method getActiveShaderPackName = irisApiClass.getMethod("getActiveShaderPackName");
                Object value = getActiveShaderPackName.invoke(api);
                if (value instanceof String name && containsAddonName(name)) {
                    return true;
                }
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean containsAddonName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("orbitalrailgun-addon") || lower.contains("orbitalrailgun addon") || lower.contains("orbitalrailgun-addon.zip");
    }
}

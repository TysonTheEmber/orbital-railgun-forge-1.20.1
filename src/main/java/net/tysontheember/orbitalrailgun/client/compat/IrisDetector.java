package net.tysontheember.orbitalrailgun.client.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tysontheember.orbitalrailgun.config.ClientConfig;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class IrisDetector {
    private static final Logger LOG = LogUtils.getLogger();
    private static boolean cached;
    private static boolean value;

    private IrisDetector() {
    }

    public static boolean isShaderPackEnabled() {
        if (!cached) {
            boolean irisPresent = isClassPresent("net.irisshaders.iris.shaderpack.ShaderPack");
            boolean oculusPresent = isClassPresent("org.anarres.oculus.Oculus");
            boolean enabled = false;
            try {
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, IrisDetector.class.getClassLoader());
                Object api = irisApi.getMethod("getInstance").invoke(null);
                enabled = (boolean) irisApi.getMethod("isShaderPackInUse").invoke(api);
            } catch (Throwable ignored) {
            }
            value = irisPresent && enabled;
            cached = true;
            if (ClientConfig.COMPAT_LOG_IRIS_STATE.get()) {
                LOG.info("[orbital_railgun] Shader pack present? {}  in-use? {}  oculus? {}", irisPresent, value, oculusPresent);
            }
        }
        return value;
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, IrisDetector.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void invalidate() {
        cached = false;
    }
}

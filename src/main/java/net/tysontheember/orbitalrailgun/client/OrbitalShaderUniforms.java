package net.tysontheember.orbitalrailgun.client;

import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;

import java.util.List;

import static net.tysontheember.orbitalrailgun.client.ClientUtil.parseHexColor;

@OnlyIn(Dist.CLIENT)
public final class OrbitalShaderUniforms {
    private OrbitalShaderUniforms() {}

    /** Call this right before rendering the chain (or wherever other uniforms are set each frame). */
    public static void applyColorUniforms(List<PostPass> passes) {
        if (passes == null || passes.isEmpty()) return;

        var c = OrbitalConfig.CLIENT;

        float[] beam = parseHexColor(c.beamColorHex.get(), c.beamAlpha.get());
        float[] inner = parseHexColor(c.markerInnerHex.get(), c.markerInnerAlpha.get());
        float[] outer = parseHexColor(c.markerOuterHex.get(), c.markerOuterAlpha.get());
        float[] marker = new float[] {
                (inner[0] + outer[0]) * 0.5F,
                (inner[1] + outer[1]) * 0.5F,
                (inner[2] + outer[2]) * 0.5F,
                Math.max(inner[3], outer[3])
        };

        for (PostPass pass : passes) {
            EffectInstance shader = pass.getEffect();
            if (shader == null) continue;

            set3f(shader, "u_BeamColor", beam[0], beam[1], beam[2]);
            set1f(shader, "u_BeamAlpha", beam[3]);

            set3f(shader, "u_MarkerInnerColor", inner[0], inner[1], inner[2]);
            set1f(shader, "u_MarkerInnerAlpha", inner[3]);

            set3f(shader, "u_MarkerOuterColor", outer[0], outer[1], outer[2]);
            set1f(shader, "u_MarkerOuterAlpha", outer[3]);

            set4f(shader, "u_MarkerColor", marker[0], marker[1], marker[2], marker[3]);
        }
    }

    private static void set1f(EffectInstance shader, String name, float v) {
        var u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void set3f(EffectInstance shader, String name, float x, float y, float z) {
        var u = shader.getUniform(name);
        if (u != null) u.set(x, y, z);
    }

    private static void set4f(EffectInstance shader, String name, float x, float y, float z, float w) {
        var u = shader.getUniform(name);
        if (u != null) u.set(x, y, z, w);
    }
}

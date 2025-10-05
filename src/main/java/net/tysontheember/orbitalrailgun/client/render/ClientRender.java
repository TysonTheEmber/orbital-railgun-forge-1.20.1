package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public final class ClientRender {
    public static ShaderInstance ORB_BEAM_SHADER;
    public static ShaderInstance ORB_MARKER_SHADER;

    public static final ResourceLocation MASK_TEX = new ResourceLocation("orbital_railgun", "textures/mask/mask.png");

    public static final class Uniforms {
        public static Uniform uTime;
        public static Uniform uCharge;
        public static Uniform uHitKind;
        public static Uniform uDistance;
        public static Uniform uIntensity;

        private Uniforms() {
        }
    }

    private ClientRender() {
    }
}

#version 150

in vec4 vColor;
in vec2 vUV;

uniform sampler2D MaskTex;

// Keep these so MC/Embeddium/Oculus stop warning when trying to upload them.
uniform vec4 ColorModulator;
uniform float GameTime;
uniform vec2 ScreenSize;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

out vec4 fragColor;

void main() {
    float m = texture(MaskTex, vUV).r;
    vec4 col = vColor * ColorModulator;
    col.a *= m;

    // simple fog (optional; very conservative)
    // not strictly required for correctness
    fragColor = col;
}

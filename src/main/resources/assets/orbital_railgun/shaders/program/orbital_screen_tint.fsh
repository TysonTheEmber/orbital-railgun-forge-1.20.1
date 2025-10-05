#version 150

in vec2 vUv;

uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform vec3 EffectPos;
uniform float Distance;
uniform vec2 ScreenSize;
uniform int HasGrab;

out vec4 FragColor;

void main() {
    vec2 centered = vUv - 0.5;
    float dist = length(centered);
    float vignette = smoothstep(0.95, 0.35, dist);
    float flash = Flash01 * (1.0 - dist) * 0.8;
    float pulse = sin(Time * 12.0) * 0.25 + 0.75;

    vec3 flashColor = mix(vec3(0.4, 0.8, 1.0), vec3(1.2, 0.8, 0.4), clamp(float(HitKind) / 2.0, 0.0, 1.0));
    vec3 color = flashColor * (flash + pulse * 0.1);
    float alpha = clamp(vignette * (flash + 0.1), 0.0, 1.0);

    FragColor = vec4(color, alpha);
}

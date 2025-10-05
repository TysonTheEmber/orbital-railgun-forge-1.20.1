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

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 centered = vUv - 0.5;
    float dist = length(centered);
    float wave = sin(dist * 32.0 - Time * 8.0);
    float pulse = smoothstep(0.6, 0.0, dist) * (0.4 + Flash01 * 0.6);
    float chroma = wave * 0.1 + Flash01 * 0.5;

    vec3 tint = vec3(0.2, 0.6, 1.0);
    vec3 heat = vec3(1.2, 0.6, 0.2);
    vec3 base = mix(tint, heat, clamp(float(HitKind) / 2.0, 0.0, 1.0));

    float flicker = hash(centered * (Time + 1.0)) * 0.15;
    vec3 color = base * (pulse + chroma + flicker);
    float alpha = clamp(pulse * 0.75 + Flash01 * 0.5, 0.0, 1.0);

    FragColor = vec4(color, alpha);
}

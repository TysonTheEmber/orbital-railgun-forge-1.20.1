#version 150
out vec4 fragColor;

uniform float Time;
uniform float Flash01;
uniform int   HitKind;
uniform vec3  HitPos;
uniform float Distance;
uniform vec2  ScreenSize;
uniform int   HasGrab;

void main() {
    // NDC from gl_FragCoord
    vec2 uv = gl_FragCoord.xy / max(ScreenSize, 1.0);

    // Simple procedural “fake” distortion / chroma hint so it compiles and shows something.
    float w = 0.003 + 0.004 * clamp(Distance / 128.0, 0.0, 1.0);
    float s = sin(Time * 2.3 + uv.y * 30.0) * 0.5 + 0.5;
    float vign = smoothstep(1.2, 0.2, length(uv * 2.0 - 1.0));

    // Base grayscale + flash tint — replace later with your real math
    vec3 base = vec3(0.0);
    base += vec3(0.2 + w * 10.0 * s) * vign;
    base += vec3(Flash01) * 0.6;

    // HitKind gives a slight hue shift
    float hk = float(HitKind % 3);
    vec3 tint = mix(base, base.bgr, hk * 0.33);

    fragColor = vec4(tint, 1.0);
}

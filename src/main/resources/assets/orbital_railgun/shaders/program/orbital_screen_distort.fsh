#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform float Distance;
uniform vec2 ScreenSize;
uniform int HasGrab;

in vec2 vUV;

out vec4 fragColor;

vec3 sampleColor(vec2 uv) {
    return texture(DiffuseSampler, uv).rgb;
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float intensity = Flash01 + float(HitKind != 0) * 0.35;
    float wave = sin(Time * 6.28318 + uv.y * 32.0) * 0.0025;
    vec2 toCenter = vec2(0.5) - uv;
    float len = max(length(toCenter), 0.0001);
    vec2 dir = toCenter / len;
    vec2 aberr = dir * (0.0015 + 0.0025 * intensity) + vec2(wave, -wave);

    vec3 colorR = sampleColor(uv + aberr * 0.75);
    vec3 colorG = sampleColor(uv);
    vec3 colorB = sampleColor(uv - aberr * 0.75);
    vec3 combined = vec3(colorR.r, colorG.g, colorB.b);

    float vignette = smoothstep(1.15, 0.35, length(uv - 0.5));
    float haze = exp(-Distance * 0.0015) * 0.3;
    combined = mix(combined, vec3(0.9, 0.6, 0.4), intensity * 0.2 + haze);
    combined *= vignette;

    float flash = smoothstep(0.0, 0.8, intensity);
    combined += flash * vec3(0.25, 0.18, 0.12);

    fragColor = vec4(combined, 1.0);
}

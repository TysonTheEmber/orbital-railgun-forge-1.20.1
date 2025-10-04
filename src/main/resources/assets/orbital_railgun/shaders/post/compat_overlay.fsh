#version 150

uniform float iTime;
uniform float StrikeActive;
uniform float SelectionActive;
uniform float Distance;
uniform int HitKind;
uniform vec2 OutSize;

in vec2 texCoord;

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

void main() {
    float activity = saturate(StrikeActive + SelectionActive);
    if (activity <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    float aspect = OutSize.y > 0.0 ? OutSize.x / OutSize.y : 1.0;
    vec2 centered = vec2((texCoord.x - 0.5) * aspect, texCoord.y - 0.5) * 2.0;
    float radius = length(centered);
    float vignette = 1.0 - smoothstep(0.45, 1.2, radius);
    float cross = exp(-28.0 * min(centered.x * centered.x, centered.y * centered.y));
    float distanceFade = saturate(1.0 - Distance / 512.0);
    float pulse = 0.6 + 0.4 * sin(iTime * 4.0);

    float strikeMix = saturate(StrikeActive);
    float hitMix = HitKind > 0 ? 1.0 : 0.0;
    vec3 chargeColor = vec3(0.25, 0.85, 1.05);
    vec3 strikeColor = vec3(1.0, 0.62, 0.28);
    vec3 hitColor = vec3(1.0, 0.38, 0.42);
    vec3 baseColor = mix(chargeColor, strikeColor, strikeMix);
    baseColor = mix(baseColor, hitColor, hitMix);

    float intensity = activity * pulse * (0.6 * vignette + 0.4 * cross) * (0.35 + 0.65 * distanceFade);
    intensity = saturate(intensity);

    fragColor = vec4(baseColor * intensity, intensity * 0.9);
}

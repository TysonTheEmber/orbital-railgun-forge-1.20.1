#version 330 compatibility

uniform float iTime;
uniform float StrikeActive;
uniform float SelectionActive;
uniform float Distance;
uniform float IsBlockHit;
uniform int HitKind;
uniform vec2 OutSize;

in vec2 vUv;

out vec4 fragColor;

void main() {
    vec2 size = vec2(max(OutSize.x, 1.0), max(OutSize.y, 1.0));
    vec2 aspect = vec2(size.x / size.y, 1.0);
    vec2 centered = (vUv * 2.0 - 1.0) * aspect;
    float radius = length(centered);

    float strikeMix = clamp(StrikeActive, 0.0, 1.0);
    float chargeMix = clamp(SelectionActive, 0.0, 1.0);
    float active = max(strikeMix, chargeMix);
    float pulse = 0.5 + 0.5 * sin(iTime * 4.0);

    vec3 strikeColor = vec3(1.0, 0.36, 0.10);
    vec3 chargeColor = vec3(0.25, 0.70, 1.00);
    vec3 color = mix(chargeColor, strikeColor, strikeMix);

    float vignette = pow(clamp(1.0 - radius, 0.0, 1.0), 2.0);
    float cross = exp(-32.0 * dot(centered, centered));
    float distanceFade = clamp(1.0 - Distance / 128.0, 0.0, 1.0);

    float alpha = active * vignette * (0.35 + 0.35 * pulse);
    alpha *= mix(1.0, distanceFade, strikeMix);
    alpha += cross * (0.2 + 0.25 * strikeMix);

    if (IsBlockHit > 0.5) {
        float flash = 0.25 + 0.35 * pulse;
        color = mix(color, vec3(1.0, 0.9, 0.5), 0.5);
        alpha += flash;
    }

    if (HitKind > 0) {
        alpha += 0.1;
    }

    alpha = clamp(alpha, 0.0, 1.0);
    fragColor = vec4(color * (0.7 + 0.3 * pulse), alpha);
}

#version 150

in vec2 texCoord;

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float iTime;
uniform float Distance;
uniform float IsBlockHit;
uniform float StrikeActive;
uniform float SelectionActive;
uniform int HitKind;

out vec4 fragColor;

float vignette(vec2 uv) {
    vec2 centered = uv - vec2(0.5);
    float dist = length(centered);
    float falloff = smoothstep(0.7, 0.2, dist);
    return falloff;
}

float crosshairGlow(vec2 uv) {
    vec2 offset = abs(uv - vec2(0.5));
    float line = max(0.0, 0.02 - min(offset.x, offset.y));
    float pulse = 0.5 + 0.5 * sin(iTime * 3.0);
    return line * pulse;
}

vec3 strikeColor() {
    float kind = clamp(float(HitKind), 0.0, 3.0) / 3.0;
    vec3 strikeTint = mix(vec3(0.15, 0.45, 1.0), vec3(1.0, 0.55, 0.1), kind);
    return strikeTint;
}

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    float overlayStrength = max(StrikeActive * 0.85, SelectionActive * 0.6);
    if (overlayStrength <= 0.0001) {
        fragColor = base;
        return;
    }

    float vignetteMask = vignette(texCoord) * overlayStrength;
    float glow = crosshairGlow(texCoord) * overlayStrength;
    float strikePulse = 0.35 + 0.65 * sin(iTime * 2.0);
    float hitBoost = mix(0.0, 0.75, clamp(IsBlockHit, 0.0, 1.0));
    vec3 tint = strikeColor();

    vec3 overlay = tint * (vignetteMask * strikePulse + glow + hitBoost);
    fragColor = vec4(base.rgb + overlay, 1.0);
}

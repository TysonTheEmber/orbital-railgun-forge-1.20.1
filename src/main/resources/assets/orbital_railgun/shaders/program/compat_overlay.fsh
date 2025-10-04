#version 330 compatibility

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;
uniform vec3 BlockPosition;
uniform vec3 HitPos;
uniform float iTime;
uniform float StrikeActive;
uniform float SelectionActive;
uniform float Distance;
uniform float IsBlockHit;
uniform int HitKind;

in vec2 texCoord;
in float viewWidth;
in float viewHeight;

out vec4 fragColor;

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 homPos = InverseTransformMatrix * vec4(ndc, 1.0);
    float invW = homPos.w == 0.0 ? 1.0 : 1.0 / homPos.w;
    vec3 viewPos = homPos.xyz * invW;
    return (inverse(ModelViewMat) * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

float softVignette(vec2 uv) {
    float len = dot(uv, uv);
    return smoothstep(1.0, 0.2, len);
}

void main() {
    vec3 baseColor = texture(DiffuseSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;

    float strikeFactor = clamp(StrikeActive, 0.0, 1.0);
    float chargeFactor = clamp(SelectionActive, 0.0, 1.0);
    float distanceFactor = clamp(1.0 / (1.0 + Distance * 0.05), 0.25, 1.0);

    vec3 overlayColor = baseColor;

    if (strikeFactor > 0.0) {
        vec3 worldDepth = worldPos(vec3(texCoord, depth)) - BlockPosition;
        float radial = length(worldDepth.xz);
        float pulse = 0.6 + 0.4 * sin(iTime * 2.5);
        float glow = exp(-radial * 0.04) * pulse;
        overlayColor += vec3(0.35, 0.7, 1.0) * glow * strikeFactor;
    }

    if (chargeFactor > 0.0) {
        vec3 towardsHit = worldPos(vec3(texCoord, depth)) - HitPos;
        float focus = exp(-dot(towardsHit, towardsHit) * 0.02);
        overlayColor = mix(overlayColor, overlayColor + vec3(0.12, 0.6, 0.28) * focus, chargeFactor * 0.75);
    }

    vec2 centered = texCoord - vec2(0.5);
    float aspect = viewHeight <= 0.0 ? 1.0 : viewWidth / viewHeight;
    centered.x *= aspect;

    float cross = max(0.0, 0.025 - abs(centered.x)) + max(0.0, 0.025 - abs(centered.y));
    float crossPulse = 0.5 + 0.5 * sin(iTime * 6.28318);
    float crossHighlight = cross * (0.6 + 0.4 * crossPulse) * distanceFactor;

    float ring = exp(-dot(centered, centered) * 24.0);
    float hitBoost = clamp(IsBlockHit + strikeFactor + chargeFactor, 0.0, 1.0);

    vec3 hitTint = mix(vec3(0.18, 0.8, 0.45), vec3(0.45, 0.7, 1.0), strikeFactor);
    if (HitKind == 2) {
        hitTint = vec3(0.9, 0.55, 0.3);
    }

    overlayColor = mix(overlayColor, overlayColor + hitTint * (ring + crossHighlight), clamp(hitBoost, 0.0, 1.0));

    float vignette = softVignette(centered * 1.35);
    overlayColor *= vignette;

    fragColor = vec4(overlayColor, 1.0);
}

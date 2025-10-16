#version 330 core

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;
uniform vec3 BlockPosition;

uniform float iTime;
uniform float StrikeActive;
uniform float StrikeRadius;

uniform vec3  u_BeamColor;
uniform float u_BeamAlpha;
uniform vec3  u_MarkerInnerColor;
uniform float u_MarkerInnerAlpha;
uniform vec3  u_MarkerOuterColor;
uniform float u_MarkerOuterAlpha;

in vec2 texCoord;

out vec4 fragColor;

const float SQRT_TWO = 1.41421356;

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 homPos = InverseTransformMatrix * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / max(homPos.w, 0.0001);
    return (inverse(ModelViewMat) * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

vec2 faceUv(vec3 local) {
    vec3 absLocal = abs(local);
    if (absLocal.x >= absLocal.y && absLocal.x >= absLocal.z) {
        return local.zy;
    }
    if (absLocal.y >= absLocal.x && absLocal.y >= absLocal.z) {
        return local.xz;
    }
    return local.xy;
}

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    if (StrikeActive < 0.5) {
        fragColor = base;
        return;
    }

    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.9999) {
        fragColor = base;
        return;
    }

    vec3 world = worldPos(vec3(texCoord, depth));
    vec3 local = world - BlockPosition;
    vec3 absLocal = abs(local);
    float maxComponent = max(absLocal.x, max(absLocal.y, absLocal.z));

    if (maxComponent > 0.51) {
        fragColor = base;
        return;
    }

    vec2 plane = faceUv(local) / 0.5;
    float radius = length(plane);
    float normalized = clamp(radius / SQRT_TWO, 0.0, 1.0);
    float radiusScale = clamp(StrikeRadius / 6.0, 0.25, 4.0);
    normalized = clamp(normalized / radiusScale, 0.0, 1.0);

    float pulse = 0.5 + 0.5 * sin(iTime * 4.0);
    float innerMask = smoothstep(1.0, 0.0, normalized) * pulse;
    float outerMask = smoothstep(0.4, 1.0, normalized);

    float innerAlpha = innerMask * u_MarkerInnerAlpha;
    float outerAlpha = outerMask * u_MarkerOuterAlpha;
    float alpha = clamp(innerAlpha + outerAlpha, 0.0, 1.0);

    vec3 highlight = u_MarkerInnerColor * innerAlpha + u_MarkerOuterColor * outerAlpha;

    if (alpha <= 0.0001) {
        fragColor = base;
        return;
    }

    fragColor = vec4(highlight, alpha);
}

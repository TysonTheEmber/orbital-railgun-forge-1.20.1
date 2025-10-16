#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;
uniform vec3 BlockPosition;
uniform float iTime;
uniform float StrikeRadius;
uniform vec3  u_MarkerInnerColor;
uniform float u_MarkerInnerAlpha;
uniform vec3  u_MarkerOuterColor;
uniform float u_MarkerOuterAlpha;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;

vec3 toView(vec3 ndc) {
    vec4 homPos = InverseTransformMatrix * vec4(ndc, 1.0);
    return homPos.xyz / max(homPos.w, 0.00001);
}

vec3 worldPos(vec3 sampleCoord) {
    vec3 ndc = sampleCoord * 2.0 - 1.0;
    vec3 viewPos = toView(ndc);
    return (inverse(ModelViewMat) * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

float pulse01(float t) {
    return 0.5 + 0.5 * sin(t);
}

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);

    float depth = texture(DepthSampler, texCoord).r;

    mat4 projMat = inverse(InverseTransformMatrix);
    vec3 relativeBlock = BlockPosition - CameraPosition;
    vec4 blockView = ModelViewMat * vec4(relativeBlock, 1.0);
    vec4 blockClip = projMat * blockView;

    if (blockClip.w <= 0.0) {
        fragColor = base;
        return;
    }

    vec3 blockNdc = blockClip.xyz / blockClip.w;
    vec2 blockUv = blockNdc.xy * 0.5 + 0.5;

    if (blockUv.x < -0.1 || blockUv.x > 1.1 || blockUv.y < -0.1 || blockUv.y > 1.1) {
        fragColor = base;
        return;
    }

    vec3 world = worldPos(vec3(texCoord, depth));
    float distanceToBlock = length(world - BlockPosition);

    float timeSeconds = iTime;
    float pul = pulse01(timeSeconds * 2.0 * PI);

    float radiusScale = clamp(StrikeRadius, 0.25, 12.0);
    float ringRadius = 0.18 + 0.03 * radiusScale;
    float ringWidth = (0.05 + 0.02 * pul) * clamp(radiusScale, 1.0, 6.0) / 3.0;
    float screenDistance = length(texCoord - blockUv);
    float screenRing = smoothstep(ringWidth, ringWidth * 0.25, abs(screenDistance - ringRadius));

    float fadeDepth = smoothstep(radiusScale * 0.35, 0.0, distanceToBlock);
    float intensity = screenRing * fadeDepth;

    vec3 color = mix(u_MarkerOuterColor, u_MarkerInnerColor, pul);
    float alpha = mix(u_MarkerOuterAlpha, u_MarkerInnerAlpha, pul);

    float blendedAlpha = clamp(alpha * intensity, 0.0, 1.0);
    fragColor = mix(base, vec4(color, 1.0), blendedAlpha);
}

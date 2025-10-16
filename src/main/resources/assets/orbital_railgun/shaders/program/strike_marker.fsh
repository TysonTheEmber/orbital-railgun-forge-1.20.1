#version 330 core

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 ProjMat;
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

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    if (StrikeActive < 0.5) {
        fragColor = base;
        return;
    }

    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.999) {
        fragColor = base;
        return;
    }

    vec3 relative = BlockPosition - CameraPosition;
    vec4 view = ModelViewMat * vec4(relative, 1.0);
    vec4 clip = ProjMat * view;

    if (clip.w <= 0.0) {
        fragColor = base;
        return;
    }

    vec2 ndc = clip.xy / clip.w;
    vec2 screenPos = ndc * 0.5 + 0.5;

    float distance = length(texCoord - screenPos);
    float radiusScale = clamp(StrikeRadius / 6.0, 0.25, 4.0);
    float normalized = clamp(distance / (0.12 * radiusScale), 0.0, 1.0);

    float pulse = 0.5 + 0.5 * sin(iTime * 4.0);
    float innerMask = smoothstep(1.0, 0.0, normalized) * pulse;
    float outerMask = smoothstep(0.4, 1.1, normalized);

    float innerAlpha = innerMask * u_MarkerInnerAlpha;
    float outerAlpha = outerMask * u_MarkerOuterAlpha;
    float alpha = clamp(innerAlpha + outerAlpha, 0.0, 1.0);

    if (alpha <= 0.0001) {
        fragColor = base;
        return;
    }

    vec3 highlight = u_MarkerInnerColor * innerAlpha + u_MarkerOuterColor * outerAlpha;
    fragColor = vec4(highlight, alpha);
}

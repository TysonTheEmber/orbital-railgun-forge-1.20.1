#version 150

in vec2 texCoord;

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform float frameTimeCounter;

out vec4 outColor;

// Vertex color encoding from geometry: r=charge, g=hitKind, b=distance, a=intensity
void main() {
    vec2 uv = texCoord;
    vec4 scene = texture(colortex0, uv);
    vec4 mask = texture(colortex1, uv);

    float intensity = clamp(mask.a, 0.0, 1.0);
    if (intensity <= 0.001) {
        outColor = scene;
        return;
    }

    float charge = mask.r;
    float distance = mask.b;

    float wobble = 0.01 + 0.05 * intensity;
    vec2 offset = vec2(
        sin(frameTimeCounter * 1.5 + charge * 12.0),
        cos(frameTimeCounter * 0.75 + distance * 8.0)
    ) * wobble;

    vec4 warped = texture(colortex0, uv + offset);
    outColor = mix(scene, warped, intensity);
}

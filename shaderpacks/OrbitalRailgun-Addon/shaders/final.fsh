#version 150

in vec2 texCoord;

uniform sampler2D colortex0;
uniform sampler2D colortex1;

out vec4 outColor;

void main() {
    vec2 uv = texCoord;
    vec4 scene = texture(colortex0, uv);
    vec4 mask = texture(colortex1, uv);

    float intensity = clamp(mask.a, 0.0, 1.0);
    float vignette = 1.0 - dot(uv * 2.0 - 1.0, uv * 2.0 - 1.0);
    vignette = clamp(vignette, 0.0, 1.0);
    float flash = intensity * 0.35 * vignette;
    vec3 colorBoost = vec3(1.0 + flash * 0.5, 1.0 + flash * 0.35, 1.0 + flash * 0.2);

    outColor = vec4(scene.rgb * colorBoost, scene.a);
}

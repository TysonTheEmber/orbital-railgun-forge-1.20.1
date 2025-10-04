#version 150

in vec2 texCoord;

uniform sampler2D DiffuseSampler;
uniform float VignetteStrength;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    vec2 centered = texCoord - vec2(0.5);
    float vignette = 1.0 - VignetteStrength * dot(centered, centered) * 2.0;
    fragColor = vec4(color.rgb * clamp(vignette, 0.0, 1.0), color.a);
}

#version 150

uniform sampler2D DiffuseSampler; // provided by PostChain
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Time;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 base = texture(DiffuseSampler, texCoord0);

    // Subtle vignette (visual proof-of-life for the effect)
    float d = distance(texCoord0, vec2(0.5));
    float vignette = smoothstep(0.9, 0.2, d);

    fragColor = base * vec4(vec3(vignette), 1.0);
}

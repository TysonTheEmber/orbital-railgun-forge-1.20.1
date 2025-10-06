#version 150

uniform sampler2D Sampler0;

in vec4 vColor;
in vec2 vUv;

uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vUv);
    vec4 base = tex * vColor * ColorModulator;
    fragColor = base;
}

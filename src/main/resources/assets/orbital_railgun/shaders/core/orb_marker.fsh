#version 150

in vec4 vColor;
in vec2 vUV;

uniform sampler2D MaskTex;
uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    float m = texture(MaskTex, vUV).r;
    vec4 col = vColor * ColorModulator;
    col.a *= m;
    fragColor = col;
}

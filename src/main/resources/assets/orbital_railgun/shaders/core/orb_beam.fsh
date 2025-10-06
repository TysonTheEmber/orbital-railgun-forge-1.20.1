#version 150

uniform sampler2D Sampler0; // single bound texture from RenderType (your mask)

in vec4 vColor;
in vec2 vUv;

uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vUv);
    vec4 base = tex * vColor * ColorModulator;
    // If your mask is grayscale in the red channel, use its alpha:
    float a = tex.a;
    fragColor = vec4(base.rgb, base.a * a);
}

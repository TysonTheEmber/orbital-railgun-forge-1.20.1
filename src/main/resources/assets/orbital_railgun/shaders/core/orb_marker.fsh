#version 150

in vec4 vColor;
in vec2 vUV;

uniform sampler2D MaskTex;

out vec4 fragColor;

void main() {
    float m = texture(MaskTex, vUV).r;
    // Slightly brighter marker so it pops
    vec3 col = vColor.rgb * 1.25;
    fragColor = vec4(col, vColor.a * m);
}

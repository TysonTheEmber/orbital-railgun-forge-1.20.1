#version 150

in vec4 vColor;
in vec2 vUV;

uniform sampler2D MaskTex;

out vec4 fragColor;

void main() {
    // White mask means “fully on” (keep it simple)
    float m = texture(MaskTex, vUV).r;
    fragColor = vec4(vColor.rgb, vColor.a * m);
}

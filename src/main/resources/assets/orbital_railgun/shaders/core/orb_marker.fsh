#version 150

uniform sampler2D Sampler0;

in vec4 vColor;
in vec2 vUv;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vUv);
    fragColor = vec4(vColor.rgb * tex.rgb, vColor.a * tex.a);
}

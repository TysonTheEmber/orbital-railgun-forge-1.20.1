#version 150
in vec4 vColor;
in vec2 vUV;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vUV);
    // beam tint from vertex color; texture can be a 1x1 white mask
    fragColor = vec4(tex.rgb * vColor.rgb, tex.a * vColor.a);
}

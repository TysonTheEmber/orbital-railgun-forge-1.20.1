#version 150
in vec4 vColor;
in vec2 vUV;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    // Write mostly to alpha so the shader-pack can pick it up as a mask.
    vec4 tex = texture(Sampler0, vUV);
    float mask = clamp(vColor.a, 0.0, 1.0);
    fragColor = vec4(0.0, 0.0, 0.0, tex.a * mask);
}

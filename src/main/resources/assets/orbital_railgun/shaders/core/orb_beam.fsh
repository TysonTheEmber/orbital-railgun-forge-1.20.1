#version 150
in vec4 vColor;
in vec2 vUV;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    // Invisible in vanilla color; Iris/Oculus reads vColor via gbuffers.
    fragColor = vec4(0.0, 0.0, 0.0, 0.0);
}

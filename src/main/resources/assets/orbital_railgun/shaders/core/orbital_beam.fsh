#version 150
out vec4 fragColor;

uniform float Time;
uniform float Charge01;

void main() {
    float pulse = 0.6 + 0.4 * sin(Time * 10.0);
    float intensity = mix(0.4, 1.0, Charge01) * pulse;
    fragColor = vec4(intensity, intensity * 0.8, 0.6 * intensity, 1.0);
}

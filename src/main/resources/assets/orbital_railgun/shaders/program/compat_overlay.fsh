#version 150

in vec2 vUV;

uniform float uTime;
uniform float uIntensity;

out vec4 FragColor;

void main() {
    vec2 uv = vUV * 2.0 - 1.0;
    float r = length(uv);
    float vignette = smoothstep(1.1, 0.2, r);
    float pulse = 0.5 + 0.5 * sin(uTime * 2.0);
    FragColor = vec4(0.0, 0.0, 0.0, (1.0 - vignette) * 0.35 * uIntensity * pulse);
}

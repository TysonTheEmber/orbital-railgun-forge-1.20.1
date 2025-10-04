#version 150
in vec2 vUV;
uniform float uTime;
uniform float uIntensity;
out vec4 FragColor;
void main() {
  // Simple vignette pulse — replace with your effect
  vec2 uv = vUV * 2.0 - 1.0;
  float r = length(uv);
  float vign = smoothstep(1.15, 0.3, r);
  float pulse = 0.5 + 0.5 * sin(uTime * 2.0);
  FragColor = vec4(0.0, 0.0, 0.0, (1.0 - vign) * 0.35 * uIntensity * pulse);
}

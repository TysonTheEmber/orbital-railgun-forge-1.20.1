#version 150
in vec2 vUV;
uniform float uTime;
uniform float StrikeActive;
uniform float SelectionActive;
out vec4 FragColor;
void main() {
  vec2 uv = vUV * 2.0 - 1.0;
  float r = length(uv);
  float vign = smoothstep(1.2, 0.25, r);
  float pulse = 0.5 + 0.5 * sin(uTime * 2.2);
  float active = max(StrikeActive, SelectionActive);
  float alpha = (1.0 - vign) * 0.35 * pulse * active;
  FragColor = vec4(1.0, 0.0, 1.0, alpha);
}

#version 150
in vec3 Position;
in vec2 UV0;
uniform vec2 OutSize;
out vec2 vUV;
void main() {
  vUV = UV0;
  // Position.xy are GUI pixel coords; map to [-1,1] clip space
  vec2 ndc = (Position.xy / OutSize) * 2.0 - 1.0;
  gl_Position = vec4(ndc, 0.0, 1.0);
}

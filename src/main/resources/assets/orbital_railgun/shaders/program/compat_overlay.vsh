#version 150

in vec3 Position;
in vec2 UV0;
out vec2 vUV;

void main() {
    vUV = UV0;
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}

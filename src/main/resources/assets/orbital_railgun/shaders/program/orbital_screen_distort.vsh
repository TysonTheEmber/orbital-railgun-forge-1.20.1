#version 150

in vec3 Position;

out vec2 vUV;

void main() {
    gl_Position = vec4(Position, 1.0);
    vUV = Position.xy * 0.5 + 0.5;
}

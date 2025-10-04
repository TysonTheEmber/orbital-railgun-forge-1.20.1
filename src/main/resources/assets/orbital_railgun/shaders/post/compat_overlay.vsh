#version 330 compatibility

in vec3 Position;
out vec2 vUv;

void main() {
    vUv = (Position.xy + 1.0) * 0.5;
    gl_Position = vec4(Position, 1.0);
}

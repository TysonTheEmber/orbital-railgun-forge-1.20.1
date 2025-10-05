#version 150
in vec3 Position;

void main() {
    // Full-screen triangle already in clip space
    gl_Position = vec4(Position, 1.0);
}

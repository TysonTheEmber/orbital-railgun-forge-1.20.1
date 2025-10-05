#version 150

in vec3 Position;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

out float vHeight;

void main() {
    vec4 world = ModelViewMat * vec4(Position, 1.0);
    vHeight = Position.y;
    gl_Position = ProjMat * world;
}

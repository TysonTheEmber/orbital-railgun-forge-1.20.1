#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out float vHeight;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vHeight = Position.y;
}

#version 150

in vec3 Position;
in vec4 Color;

out vec4 vColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    vColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}

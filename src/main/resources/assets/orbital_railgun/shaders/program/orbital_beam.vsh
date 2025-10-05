#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMatrix;
uniform mat4 ProjectionMatrix;

out vec4 vColor;
out vec2 vUV;

void main() {
    gl_Position = ProjectionMatrix * ModelViewMatrix * vec4(Position, 1.0);
    vColor = Color;
    vUV = UV0;
}

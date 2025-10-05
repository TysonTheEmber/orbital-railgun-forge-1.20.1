#version 150

in vec2 vaPosition;
in vec2 vaUV0;

out vec2 texCoord;

void main() {
    texCoord = vaUV0;
    gl_Position = vec4(vaPosition.xy, 0.0, 1.0);
}

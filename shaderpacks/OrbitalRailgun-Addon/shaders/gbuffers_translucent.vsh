#version 150

in vec4 vaPosition;
in vec2 vaUV0;
in vec4 vaColor;

uniform mat4 gbufferModelView;
uniform mat4 gbufferProjection;

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    texCoord = vaUV0;
    vertexColor = vaColor;
    gl_Position = gbufferProjection * gbufferModelView * vaPosition;
}

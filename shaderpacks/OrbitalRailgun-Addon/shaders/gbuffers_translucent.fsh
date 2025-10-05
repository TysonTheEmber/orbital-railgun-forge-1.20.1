#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform sampler2D gtexture;

layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outMask;

void main() {
    vec4 tex = texture(gtexture, texCoord);
    vec4 encoded = vertexColor * tex;
    outColor = vec4(0.0);
    outMask = encoded;
}

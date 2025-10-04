#version 330 compatibility

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;
out float viewWidth;
out float viewHeight;

void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.0, 1.0);

    texCoord = Position.xy / OutSize;
    viewWidth = OutSize.x;
    viewHeight = OutSize.y;
}

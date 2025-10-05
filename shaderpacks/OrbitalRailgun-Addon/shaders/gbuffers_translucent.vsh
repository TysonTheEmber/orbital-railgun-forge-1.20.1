#version 120
// passthrough for engine-provided attributes
varying vec4 vColor;
varying vec2 texcoord;

void main() {
    gl_Position = ftransform();
    vColor      = gl_Color;
    texcoord    = gl_MultiTexCoord0.st;
}

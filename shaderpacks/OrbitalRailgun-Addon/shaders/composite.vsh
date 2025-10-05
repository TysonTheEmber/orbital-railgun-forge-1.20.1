#version 120
// fullscreen quad with proper UVs from the engine
varying vec2 texcoord;
void main() {
    gl_Position = ftransform();
    texcoord    = gl_MultiTexCoord0.st;
}

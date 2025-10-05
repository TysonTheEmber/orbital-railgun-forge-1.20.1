#version 150

in vec3 Position;

// Keep these declared so EffectInstance can set them
uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    // full-screen triangle already in clip space; don’t double-transform
    gl_Position = vec4(Position, 1.0);

    // Touch the matrices so they aren’t optimized out (no visible effect)
    float _guard = (ProjMat[0][0] + ModelViewMat[0][0]) * 0.0;
    gl_Position.x += _guard;
}

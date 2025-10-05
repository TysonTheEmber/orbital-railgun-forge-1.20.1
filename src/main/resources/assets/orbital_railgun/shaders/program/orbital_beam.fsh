#version 150

in vec4 vColor;

uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform float Distance;

out vec4 fragColor;

void main() {
    float flicker = sin(Time * 20.0) * 0.1 + 0.9;
    fragColor = vec4(vColor.rgb * flicker, vColor.a);
}

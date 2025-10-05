#version 150

uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform float Distance;
uniform vec2 ScreenSize;
uniform int HasGrab;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec3 tint = vec3(0.35, 0.6, 1.0);
    float ring = smoothstep(0.3, 0.0, length(uv - 0.5));
    float flash = Flash01;
    fragColor = vec4(tint * ring * flash, flash * 0.75);
}

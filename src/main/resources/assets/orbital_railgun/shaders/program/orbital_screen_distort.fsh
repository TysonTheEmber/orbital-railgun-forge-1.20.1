#version 150

uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform float Distance;
uniform vec2 ScreenSize;
uniform int HasGrab;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(41.0, 289.0))) * 43758.5453);
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float vignette = smoothstep(1.0, 0.2, length(uv - 0.5) * 1.2);
    float pulse = sin(Time * 4.0) * 0.5 + 0.5;
    float distortion = pulse * 0.05 * vignette;
    vec2 offset = (uv - 0.5) * distortion;
    float flash = Flash01 * 0.8;
    fragColor = vec4(vec3(flash) + vec3(distortion), flash * 0.5);
}

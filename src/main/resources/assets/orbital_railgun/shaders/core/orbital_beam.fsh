#version 150

uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform float Distance;

in float vHeight;

out vec4 fragColor;

void main() {
    float pulse = sin(Time * 8.0 + vHeight * 0.1) * 0.5 + 0.5;
    float falloff = clamp(1.0 - abs(vHeight) * 0.002, 0.0, 1.0);
    vec3 beamColor = mix(vec3(0.2, 0.6, 1.0), vec3(1.0, 0.8, 0.5), Flash01);
    beamColor *= pulse * falloff;
    float alpha = clamp(0.35 + Flash01 * 0.5, 0.0, 1.0) * falloff;
    fragColor = vec4(beamColor, alpha);
}

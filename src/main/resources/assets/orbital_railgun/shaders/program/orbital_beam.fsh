#version 150

in float vHeight;

uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform vec3 EffectPos;
uniform float Distance;

out vec4 FragColor;

void main() {
    float beam = clamp(1.0 - abs(fract(vHeight * 0.05 + Time * 0.5) - 0.5) * 6.0, 0.0, 1.0);
    float pulse = sin(Time * 6.0 + vHeight * 0.02) * 0.5 + 0.5;
    vec3 base = mix(vec3(0.2, 0.6, 1.0), vec3(1.1, 0.7, 0.2), clamp(float(HitKind) / 2.0, 0.0, 1.0));
    vec3 color = base * (beam * (0.6 + Flash01) + pulse * 0.3);
    float alpha = clamp(beam * (0.4 + Flash01), 0.0, 1.0);
    FragColor = vec4(color, alpha);
}

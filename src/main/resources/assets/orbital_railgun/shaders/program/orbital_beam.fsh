#version 150

uniform float Time;
uniform float Flash01;

in vec4 vColor;
in vec2 vUV;

out vec4 fragColor;

void main() {
    float beam = 1.0 - abs(vUV.y - 0.5) * 2.0;
    beam = clamp(beam, 0.0, 1.0);
    float pulse = 0.5 + 0.5 * sin(Time * 12.0 + vUV.y * 8.0);
    float flash = Flash01 * 0.75 + 0.25;
    vec3 color = vColor.rgb * (beam * pulse * flash);
    fragColor = vec4(color, beam);
}

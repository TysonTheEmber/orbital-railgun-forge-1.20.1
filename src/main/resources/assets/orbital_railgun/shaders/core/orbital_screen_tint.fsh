#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Flash01;
uniform int HitKind;
uniform vec3 HitPos;
uniform float Distance;
uniform vec2 ScreenSize;
uniform int HasGrab;

in vec2 vUv;

out vec4 fragColor;

void main() {
    vec2 uv = vUv;
    vec3 sampleColor;
    if (HasGrab == 1) {
        sampleColor = texture(DiffuseSampler, uv).rgb;
    } else {
        sampleColor = vec3(0.1);
    }

    float vignette = smoothstep(0.8, 0.1, distance(uv, vec2(0.5)));
    float glow = Flash01 * (1.0 - vignette);
    vec3 tint = vec3(0.5 + 0.5 * sin(Time + uv.xyx * 4.0));
    vec3 finalColor = mix(sampleColor, tint, 0.5) + glow * vec3(1.0, 0.8, 0.6);

    fragColor = vec4(finalColor * vignette, clamp(Flash01, 0.1, 1.0));
}

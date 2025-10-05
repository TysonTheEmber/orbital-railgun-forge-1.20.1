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

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 uv = vUv;
    float pulse = sin(Time * 2.0 + uv.y * 10.0) * 0.01;
    float radial = length(uv - 0.5);
    uv += vec2(pulse, sin(Time + uv.x * 14.0) * 0.01);
    vec3 baseColor;
    if (HasGrab == 1) {
        baseColor = texture(DiffuseSampler, uv).rgb;
    } else {
        baseColor = vec3(0.25 + 0.75 * uv.x, 0.4 + 0.4 * uv.y, 0.6 + 0.3 * sin(Time + radial * 12.0));
    }

    float vignette = smoothstep(0.75, 0.2, radial);
    float chroma = 0.005 + 0.01 * Flash01;

    vec3 distorted;
    if (HasGrab == 1) {
        distorted.r = texture(DiffuseSampler, uv + vec2(chroma, 0.0)).r;
        distorted.g = texture(DiffuseSampler, uv).g;
        distorted.b = texture(DiffuseSampler, uv - vec2(chroma, 0.0)).b;
    } else {
        distorted = baseColor;
    }

    float flash = Flash01 * (1.0 - radial);
    vec3 finalColor = mix(baseColor, distorted, 0.65) + flash * vec3(1.2, 0.9, 0.7);
    finalColor *= vignette;

    fragColor = vec4(finalColor, clamp(Flash01 * 0.4 + 0.6, 0.0, 1.0));
}

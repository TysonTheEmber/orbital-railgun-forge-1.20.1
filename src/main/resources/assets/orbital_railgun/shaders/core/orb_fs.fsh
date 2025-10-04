#version 150
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform float iTime;
uniform int   HitKind;
uniform vec3  HitPos;

out vec4 fragColor;

void main() {
    vec2 res = textureSize(SceneColor, 0);
    vec2 uv  = gl_FragCoord.xy / res;
    vec3 col = texture(SceneColor, uv).rgb;

    vec2 d = uv - 0.5;
    float vig = smoothstep(0.9, 0.2, dot(d,d));
    float flash = (HitKind > 0) ? 0.2 * exp(-fract(iTime) * 6.0) : 0.0;

    fragColor = vec4(col * vig + flash, 1.0);
}

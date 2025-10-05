#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Flash01;

in vec2 vUV;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / textureSize(DiffuseSampler, 0);
    vec4 base = texture(DiffuseSampler, uv);
    float vignette = smoothstep(1.1, 0.4, length(uv - 0.5));
    float pulse = Flash01;
    vec3 glow = vec3(1.2, 0.8, 0.4) * pulse;
    base.rgb = mix(base.rgb, glow + base.rgb, pulse * 0.6);
    base.rgb *= vignette;
    fragColor = vec4(base.rgb, 1.0);
}

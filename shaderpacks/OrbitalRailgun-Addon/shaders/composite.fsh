#version 120
uniform sampler2D colortex0;   // previous color
uniform vec2      texelSize;   // 1 / framebuffer size
uniform float     frameTimeCounter;

varying vec2 texcoord;

float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453); }

void main() {
    vec4 base  = texture2D(colortex0, texcoord);
    float mask = base.b;

    float t    = frameTimeCounter;
    vec2  dir  = normalize(vec2(sin(t*3.1), cos(t*2.7)));
    float n    = hash(floor(texcoord / texelSize * 0.5)) * 2.0 - 1.0;

    float k    = clamp(mask, 0.0, 1.0);
    vec2  off  = dir * (0.75 * k) * texelSize + n * (0.35 * k) * texelSize;

    vec3 refr  = texture2D(colortex0, texcoord + off).rgb;
    vec3 color = mix(base.rgb, refr, k);

    // preserve alpha; keep blue as mask for final if desired
    gl_FragData[0] = vec4(color.r, color.g, base.b, base.a);
}

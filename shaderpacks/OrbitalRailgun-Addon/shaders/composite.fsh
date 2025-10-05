#version 120
// minimal, pack-friendly refraction keyed by mask in BLUE channel

uniform sampler2D colortex0;   // composited color from previous stage
uniform vec2      texelSize;   // 1.0 / framebuffer size (Iris/Oculus provides this)
uniform float     frameTimeCounter;

varying vec2 texcoord;

float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453); }

void main() {
    vec4 base  = texture2D(colortex0, texcoord);

    // read our mask from blue
    float mask = base.b;

    // small, soft refraction
    float t    = frameTimeCounter;
    vec2  dir  = normalize(vec2(sin(t*3.1), cos(t*2.7)));
    float n    = hash(floor(texcoord / texelSize * 0.5)) * 2.0 - 1.0;

    // strength scales with mask, stays framebuffer-resolution-aware
    float k    = clamp(mask, 0.0, 1.0);
    vec2  off  = dir * (0.75 * k) * texelSize + n * (0.35 * k) * texelSize;

    vec3 refr  = texture2D(colortex0, texcoord + off).rgb;

    // mix only where mask > 0
    vec3 color = mix(base.rgb, refr, k);

    // preserve alpha, keep blue channel carrying mask forward in case final wants it
    gl_FragData[0] = vec4(color.r, color.g, base.b, base.a);
}

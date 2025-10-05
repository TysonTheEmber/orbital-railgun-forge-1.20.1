#version 150
out vec4 fragColor;

uniform float Time;
uniform float Flash01;
uniform vec2  ScreenSize;

void main() {
    vec2 uv = gl_FragCoord.xy / max(ScreenSize, 1.0);
    float vign = smoothstep(1.1, 0.4, length(uv * 2.0 - 1.0));
    // Subtle warm tint that brightens with Flash01
    vec3 color = mix(vec3(0.02, 0.01, 0.00), vec3(0.25, 0.10, 0.03), vign) + vec3(Flash01) * 0.3;
    fragColor = vec4(color, 0.65); // translucent tint layer
}

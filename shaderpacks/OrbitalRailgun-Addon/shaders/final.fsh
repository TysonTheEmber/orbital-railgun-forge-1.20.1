#version 120
uniform sampler2D colortex0;
uniform float     frameTimeCounter;

varying vec2 texcoord;

void main() {
    vec4 col = texture2D(colortex0, texcoord);
    float r  = distance(texcoord, vec2(0.5));
    float vig   = smoothstep(0.85, 0.2, r);
    float flash = 0.02 * (0.5 + 0.5 * sin(frameTimeCounter * 6.28318));
    col.rgb *= (1.0 + flash * vig);

    gl_FragData[0] = col;
}

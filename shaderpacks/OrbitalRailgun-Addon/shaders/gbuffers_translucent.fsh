#version 120
// encode mask into BLUE channel; engine will blend this into colortex0
varying vec4 vColor;
varying vec2 texcoord;

void main() {
    float marker = step(0.8, vColor.g);         // your builders set g>=0.8 for mask geo
    float mask   = clamp(vColor.a, 0.0, 1.0) * marker;

    // invisible in RGB; carry mask in blue. alpha 1.0 to avoid discards
    gl_FragData[0] = vec4(0.0, 0.0, mask, 1.0);
}

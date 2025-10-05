#version 120
// write a MASK into the BLUE channel so composite can read it
// convention: your mod sets vColor.g >= 0.8 on marker/beam draws, with intensity in vColor.a

varying vec4 vColor;
varying vec2 texcoord;

void main() {
    float marker  = step(0.8, vColor.g);
    float mask    = clamp(vColor.a, 0.0, 1.0) * marker;

    // keep RGB invisible; carry the mask in blue. alpha kept 1.0 to avoid early discards
    gl_FragData[0] = vec4(0.0, 0.0, mask, 1.0);
}

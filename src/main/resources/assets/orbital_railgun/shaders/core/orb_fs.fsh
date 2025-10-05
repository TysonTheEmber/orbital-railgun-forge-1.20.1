// assets/orbital_railgun/shaders/core/orb_fs.fsh
#version 150

uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;          // may or may not be bound at runtime

uniform vec2  OutSize;
uniform mat4  InverseTransformMatrix;  // provided by you (inverse projection)
uniform vec3  CameraPosition;
uniform float iTime;
uniform int   HitKind;
uniform vec3  HitPos;

out vec4 fragColor;

void main() {
    // Screen-space UVs from pixel coords
    vec2 uv = gl_FragCoord.xy / OutSize;

    vec4 color = texture(SceneColor, uv);

    // --- Zero-weight references so uniforms aren't optimized away ---
    // Depth sample (added with weight 0.0 so it doesn't change output)
    float depthSample = texture(SceneDepth, uv).r;
    color.rgb += 0.0 * depthSample;

    // Touch matrix, camera, time, hit data
    float matNibble = InverseTransformMatrix[0][0] + InverseTransformMatrix[1][1];
    color.rgb += 0.0 * matNibble;
    color.rgb += 0.0 * CameraPosition;
    color.rgb += 0.0 * iTime;
    color.rgb += 0.0 * float(HitKind);
    color.rgb += 0.0 * HitPos;

    fragColor = color;
}

#version 150
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform vec2  OutSize;
uniform mat4  InverseTransformMatrix;
uniform vec3  CameraPosition;
uniform float iTime;
uniform int   HitKind;
uniform vec3  HitPos;
uniform float StrikeActive;
uniform float SelectionActive;
uniform float Distance;
uniform mat4  ProjMat;
uniform mat4  ModelViewMat;
out vec4 fragColor;
void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 scene = texture(SceneColor, uv);
    float depth = texture(SceneDepth, uv).r;
    float pulse = 0.5 + 0.5 * sin(iTime * 6.2831853);
    vec3  tint  = mix(vec3(1.0, 0.6, 0.2), vec3(0.2, 0.6, 1.0), float(HitKind & 1));
    vec3 color = scene.rgb + 0.05 * pulse * tint;
    float guard =
    InverseTransformMatrix[0][0] * 1e-6 +
    dot(CameraPosition, HitPos)   * 0.0 +
    StrikeActive * 0.0 + SelectionActive * 0.0 + Distance * 0.0 +
    ProjMat[1][1] * 0.0 + ModelViewMat[1][1] * 0.0;
    color += vec3(guard);
    fragColor = vec4(color, scene.a);
}

#version 150
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform float     iTime;
uniform int       HitKind;
uniform vec3      HitPos;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 col = texture(SceneColor, texCoord0);
    float fxActive = (HitKind > 0) ? 1.0 : 0.0;
    float pulse    = (sin(iTime * 6.2831853) * 0.5 + 0.5) * fxActive * 0.02;
    col.rgb += pulse;
    fragColor = col;
}

precision mediump float;

varying vec2 aCoord;
uniform sampler2D vTexture;
uniform float facex;
uniform float facey;

vec2 stretchFun(vec2 textureCoord, vec2 originPosition, vec2 targetPosition, float radius, float curve)
{
    vec2 direction = targetPosition - originPosition;
    float infect = distance(textureCoord, originPosition)/radius;
    infect = 1.0 - pow(infect, curve);
    infect = clamp(infect, 0.0, 1.0);
    vec2 offset = direction * infect;
    vec2 result = textureCoord - offset;
    return result;
}

void main(){
    vec2 A1 = vec2(facex, facey);
    vec2 A2 = vec2(facex+0.05f, facey -0.05);

    vec2 TexCoord2 = stretchFun(aCoord, A1, A2, 0.19f, 2.0f);

    vec3 tmpColor = texture2D(vTexture, TexCoord2).rgb;
    gl_FragColor = vec4(tmpColor, 1.0f);
}
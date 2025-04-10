precision highp  float;
varying vec2 aCoord;
uniform sampler2D vTexture;
uniform vec2 u_texSize;//图像分辨率
uniform vec4 u_preCtrlPoints;//pre控制点
uniform vec4 u_curCtrlPoints;//cur控制点
uniform float u_reshapeRadius;//影响半径aCoord
uniform float u_reshapeRatio;//强度

vec2 face_slender_1(vec2 prePoint, vec2 curPoint, vec2 texCoord, float radius, vec2 texSize)
{
    vec2 pos = texCoord;
    vec2 newSrc = prePoint * texSize;
    vec2 newDst = curPoint * texSize;
    vec2 newTex = texCoord * texSize;
    float newRadius = radius;
    float r = distance(newSrc, newTex);

    if (r < newRadius)
    {
        float alpha = 1.0 -  r / newRadius;
        vec2 displacementVec = (newDst - newSrc) * pow(alpha, 2.0) * 0.002 * u_reshapeRatio;
        pos = (newTex - displacementVec) / texSize;
    }
    return pos;
}



vec2 face_slender_2(vec2 prePoint, vec2 curPoint, vec2 texCoord, float radius, vec2 texSize)
{
    vec2 pos = texCoord;
    vec2 newSrc = prePoint * texSize;
    vec2 newDst = curPoint * texSize;
    vec2 newTex = texCoord * texSize;
    float newRadius = radius;
    float r = distance(newSrc, newTex);

    if (r < newRadius){
        float gamma = (pow(newRadius, 2.0) - pow(r, 2.0)) / (pow(newRadius, 2.0) - pow(r, 2.0) + pow(distance(newDst, newSrc), 2.0));
        float sigma = pow(gamma, 2.0);
        vec2 displacementVec = (newDst - newSrc) * sigma * u_reshapeRatio;
        pos = (newTex - displacementVec) / texSize;
    }
    return pos;
}



void main() {
    vec2 leftPreCtrl = u_preCtrlPoints.xy;
    vec2 rightPreCtrl = u_preCtrlPoints.zw;
    vec2 leftCurCtrl = u_curCtrlPoints.xy;
    vec2 rightCurCtrl = u_curCtrlPoints.zw;
    vec2 newTexCoord = face_slender_2(leftPreCtrl, leftCurCtrl, aCoord, u_reshapeRadius, u_texSize);
    newTexCoord = face_slender_2(rightPreCtrl, rightCurCtrl, newTexCoord, u_reshapeRadius, u_texSize);
    gl_FragColor = texture2D(vTexture, newTexCoord);
}
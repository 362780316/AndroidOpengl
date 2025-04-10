//相机预览的着色器，不能直接使用 Sampler2D ，需要使用 samplerExternalOES 纹理采样器
#extension GL_OES_EGL_image_external : require

//float 数据的精度
precision mediump float;

//顶点着色器传过来的 采样点的坐标
varying vec2 aCoord;

//采样器
uniform samplerExternalOES vTexture;

void main(){
    gl_FragColor = texture2D(vTexture, aCoord);
    //黑白相机 305911 公式
//    vec4 rgba = texture2D(vTexture, aCoord);
//    float gray = rgba.r * 0.30 + rgba.g * 0.59 + rgba.b * 0.11;
//    gl_FragColor = vec4(gray, gray, gray, 1.0);
}
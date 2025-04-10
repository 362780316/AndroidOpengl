//相机预览的着色器，不能直接使用 Sampler2D ，需要使用 samplerExternalOES 纹理采样器
//#extension GL_OES_EGL_image_external : require

//float 数据的精度
precision mediump float;

//顶点着色器传过来的 采样点的坐标
varying vec2 aCoord;

//采样器
//uniform samplerExternalOES vTexture;
uniform sampler2D vTexture;

void main(){
    gl_FragColor = texture2D(vTexture, aCoord);
}
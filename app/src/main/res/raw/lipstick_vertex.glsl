//顶点坐标，用来确定要画的形状
attribute vec4 vPosition;
//纹理坐标，采样器采样图片的坐标
attribute vec2 vCoord;
varying vec2 textureCoordinate;// 图像纹理坐标
varying vec2 maskCoordinate;// 遮罩纹理坐标

void main() {
    gl_Position = vPosition;
    // 原图纹理坐标，用顶点来计算
    textureCoordinate = vPosition.xy * 0.5 + 0.5;
    // 遮罩纹理坐标，用传进来的坐标值计算
    maskCoordinate = vCoord;
}
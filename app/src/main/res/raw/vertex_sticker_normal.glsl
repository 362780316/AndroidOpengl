uniform mat4 uMVPMatrix;// 变换矩阵
attribute vec4 vPosition;// 图像顶点坐标
attribute vec4 vCoord;// 图像纹理坐标

varying vec2 textureCoordinate;// 图像纹理坐标

void main() {
    gl_Position = uMVPMatrix * vPosition;
    textureCoordinate = vCoord.xy;
}
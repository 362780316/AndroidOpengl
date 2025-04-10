//顶点坐标，用来确定要画的形状
attribute vec2 aPosition;

void main(){
    //内置变量， 把顶点数据赋值给这个变量
    gl_Position =vec4(aPosition, 0.0, 1.0);
    gl_PointSize = 10.0;
}
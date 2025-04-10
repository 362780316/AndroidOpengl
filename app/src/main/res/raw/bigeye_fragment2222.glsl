// TODO 片元着色器（大眼专用的， 局部放大的算法）

// 着色器坐标： 0 ~ 1

precision mediump float;// float 数据的精度

varying vec2 aCoord;// 顶点着色器传过来的 采样点的坐标
uniform sampler2D vTexture;// 采样器

uniform vec2 left_eye;// 左眼 x/y
uniform vec2 right_eye;// 右眼 x/y

// 着色器代码，最好加 .0，防止有问题
// 把公式转成着色器代码
// r:    原来的点 距离眼睛中心点距离（半径）
// rmax: 局部放大 最大半径 / 2
float fs(float r, float rmax) {
    float a = 0.5;// 放大系数，如果你的a==0，我会直接返回r（啥事不做）

    // 内置函数：求平方 pow
    return (1.0 - pow(r / rmax - 1.0, 2.0) * a);
}

// TODO 目的：把正常眼睛的纹理坐标，搬到 放大区域   纹理坐标搬到外面
// oldCoord 整个屏幕的纹理坐标
// eye 眼睛坐标
// rmax: 局部放大 最大半径 / 2
vec2 newCoord(vec2 oldCoord, vec2 eye, float rmax) {
    vec2 newCoord = oldCoord;
    float r = distance(oldCoord, eye);// 求两点之间的距离

    // 必须是眼睛范围才做事情，
    if (r > 0.0 && r < rmax) { // 如果进不来if，那么还是返回原来的点，啥事不做
        float fsr = fs(r, rmax);

        //    新点 - 眼睛     /  老点 - 眼睛   = 新距离;
        // (newCoord - eye) / (coord - eye) = fsr;

        // newCoord新点 =    新距离  * （老点     - 眼睛）  +  眼睛
        newCoord       =    fsr    * (oldCoord - eye)   +  eye;
    }
    return newCoord;
}

// 那个max应该是可以随便设置的吧，配置一半的限制，是为了避免两眼重叠很奇怪

void main(){
    // gl_FragColor = texture2D(vTexture, aCoord);

    // 两眼间距的一半  识别区域宽度/2吗
    float rmax = distance(left_eye, right_eye) / 2.0;// distance 求两点的距离(rmax两眼间距) 注意是放大后的间距

    // aCoord是整副图像，
    vec2 new_Coord = newCoord(aCoord, left_eye, rmax);// 求左眼放大位置的采样点
    new_Coord = newCoord(new_Coord, right_eye, rmax);// 求右眼放大位置的采样点
    // 此newCoord就是大眼像素坐标值
    gl_FragColor = texture2D(vTexture, new_Coord);
}
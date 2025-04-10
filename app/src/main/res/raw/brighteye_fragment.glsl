// 亮眼处理
precision mediump float;

varying vec2 textureCoordinate;
varying vec2 maskCoordinate;

uniform sampler2D vTexture;// 输入图像纹理
uniform sampler2D blurTexture;// 经过高斯模糊处理的图像纹理
uniform sampler2D blurTexture2;// 经过高斯模糊处理的图像纹理2
uniform sampler2D maskTexture;// 眼睛遮罩图像纹理

uniform float strength;// 明亮程度
uniform int enableProcess;// 是否允许亮眼，没有人脸时不需要亮眼处理

void main()
{
    vec4 sourceColor = texture2D(vTexture, textureCoordinate);
    vec4 maskColor = texture2D(maskTexture, maskCoordinate);
    vec4 color = sourceColor;
    if (enableProcess == 1) {
        // 如果遮罩纹理存在
        if (maskColor.r > 0.01) {
            // 高斯模糊的图像颜色值
            vec4 blurColor = texture2D(blurTexture, textureCoordinate);
            // 统计颜色
            vec3 sumColor = vec3(0.0, 0.0, 0.0);
            // 将RGB颜色差值放大。突出眼睛明亮部分
            sumColor = clamp((sourceColor.rgb - blurColor.rgb) * 12.0, 0.0, 1.0);
            sumColor = max(sourceColor.rgb, sumColor);
//结果为黑色            sumColor = max(sourceColor.rgb, sumColor*1.5);

            // 用原图和最终得到的明亮部分进行线性混合处理
//            vec3 aaa = vec3(255.0, 0.0, 0.0);
            color = mix(sourceColor, vec4(sumColor, 1.0), strength * maskColor.r);
        }
    }
    gl_FragColor = color;
//        vec4 sourceColor = texture2D(maskTexture, maskCoordinate);
//        gl_FragColor = sourceColor;
}
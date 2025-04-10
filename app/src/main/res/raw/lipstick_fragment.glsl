// 亮眼处理
precision mediump float;

varying vec2 textureCoordinate;
varying vec2 maskCoordinate;

uniform sampler2D vTexture;// 输入图像纹理
uniform sampler2D materialTexture;// 素材纹理, 对于唇彩来说，这里存放的是lut纹理
uniform sampler2D maskTexture;// 眼睛遮罩图像纹理

uniform float strength;// 明亮程度
uniform int makeupType;// 是否允许亮眼，没有人脸时不需要亮眼处理

void main()
{
    //    vec4 sourceColor = texture2D(vTexture, textureCoordinate);
    //    vec4 maskColor = texture2D(maskTexture, maskCoordinate);
    //    vec4 color = sourceColor;
    //    if (enableProcess == 1) {
    //        // 如果遮罩纹理存在
    //        if (maskColor.r > 0.01) {
    //            // 高斯模糊的图像颜色值
    //            vec4 blurColor = texture2D(blurTexture, textureCoordinate);
    //            // 统计颜色
    //            vec3 sumColor = vec3(0.0, 0.0, 0.0);
    //            // 将RGB颜色差值放大。突出眼睛明亮部分
    //            sumColor = clamp((sourceColor.rgb - blurColor.rgb) *3.0, 0.0, 1.0);
    //            sumColor = max(sourceColor.rgb, sumColor);
    ////结果为黑色            sumColor = max(sourceColor.rgb, sumColor*1.5);
    //
    //            // 用原图和最终得到的明亮部分进行线性混合处理
    ////            vec3 aaa = vec3(255.0, 0.0, 0.0);
    //            color = mix(sourceColor, vec4(sumColor, 1.0), strength * maskColor.r);
    //        }
    //    }
    ////    sumColor = vec4(1.0) - (vec4(1.0) - sourceColor) / maskColor;
    ////    color = mix(sourceColor, maskColor, maskColor.r);
    //    gl_FragColor = color;
    ////        vec4 sourceColor = texture2D(maskTexture, maskCoordinate);
    ////        gl_FragColor = sourceColor;

    if (makeupType == 0) { // 直接绘制输入的原图像纹理
        lowp vec4 sourceColor = texture2D(vTexture, textureCoordinate.xy);
        gl_FragColor = sourceColor;
    } else if (makeupType == 1) {
        lowp vec4 textureColor = texture2D(vTexture, textureCoordinate.xy);
        lowp vec4 lipMaskColor = texture2D(maskTexture, maskCoordinate.xy);
        if (lipMaskColor.r > 0.005) {
            mediump vec2 quad1;
            mediump vec2 quad2;
            mediump vec2 texPos1;
            mediump vec2 texPos2;
            mediump float blueColor = textureColor.b * 15.0;
            quad1.y = floor(floor(blueColor) / 4.0);
            quad1.x = floor(blueColor) - (quad1.y * 4.0);
            quad2.y = floor(ceil(blueColor) / 4.0);
            quad2.x = ceil(blueColor) - (quad2.y * 4.0);

            texPos1.xy = (quad1.xy * 0.25) + 0.5/64.0 + ((0.25 - 1.0/64.0) * textureColor.rg);
            texPos2.xy = (quad2.xy * 0.25) + 0.5/64.0 + ((0.25 - 1.0/64.0) * textureColor.rg);

            lowp vec3 newColor1 = texture2D(materialTexture, texPos1).rgb;
            lowp vec3 newColor2 = texture2D(materialTexture, texPos2).rgb;

            lowp vec3 newColor = mix(newColor1, newColor2, fract(blueColor));

            textureColor = vec4(newColor, 1.0) * (lipMaskColor.r * strength);
        } else {
            textureColor = vec4(0.0, 0.0, 0.0, 0.0);
        }
        gl_FragColor = textureColor;
    } else {
        gl_FragColor = texture2D(vTexture, textureCoordinate.xy);
    }
}
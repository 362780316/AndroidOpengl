package com.ray.opengl.filter.beauty;

import android.content.Context;

import com.ray.opengl.R;
import com.ray.opengl.base.GLImageGaussianBlurFilter;
import com.ray.opengl.filter.BaseFrameFilter;
import com.ray.opengl.filter.beauty.bean.BeautyParam;
import com.ray.opengl.utils.OpenGLUtils;

import java.nio.FloatBuffer;

/**
 * 实时美颜，这里用的是高反差保留磨皮法
 */
public class GLImageBeautyFilter extends BaseFrameFilter {

    // 美肤滤镜
    private GLImageBeautyComplexionFilter mComplexionFilter;
    // 高斯模糊
    private GLImageBeautyBlurFilter mBeautyBlurFilter;
    // 高通滤波
    private GLImageBeautyHighPassFilter mHighPassFilter;
    // 高通滤波做高斯模糊处理，保留边沿细节
    private GLImageGaussianBlurFilter mHighPassBlurFilter;
    // 磨皮程度调节滤镜
    private GLImageBeautyAdjustFilter mBeautyAdjustFilter;

    // 缩放
    private float mBlurScale = 0.5f;

    public GLImageBeautyFilter(Context context) {
        super(context, 0, 0);
        initFilters();
    }

    private void initFilters() {
        mComplexionFilter = new GLImageBeautyComplexionFilter(mContext);
        mBeautyBlurFilter = new GLImageBeautyBlurFilter(mContext);
        mHighPassFilter = new GLImageBeautyHighPassFilter(mContext);
        mHighPassBlurFilter = new GLImageGaussianBlurFilter(mContext);
        mBeautyAdjustFilter = new GLImageBeautyAdjustFilter(mContext);
    }


    @Override
    public void onReady(int width, int height) {
        super.onReady(width, height);
        if (mComplexionFilter != null) {
            mComplexionFilter.onInputSizeChanged(width, height);
        }
        if (mBeautyBlurFilter != null) {
            mBeautyBlurFilter.onInputSizeChanged((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
        if (mHighPassFilter != null) {
            mHighPassFilter.onInputSizeChanged((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
        if (mHighPassBlurFilter != null) {
            mHighPassBlurFilter.onInputSizeChanged((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
        if (mBeautyAdjustFilter != null) {
            mBeautyAdjustFilter.onInputSizeChanged(width, height);
        }


        if (mComplexionFilter != null) {
            mComplexionFilter.onDisplaySizeChanged(width, height);
        }
        if (mBeautyBlurFilter != null) {
            mBeautyBlurFilter.onDisplaySizeChanged(width, height);
        }
        if (mHighPassFilter != null) {
            mHighPassFilter.onDisplaySizeChanged(width, height);
        }
        if (mHighPassBlurFilter != null) {
            mHighPassBlurFilter.onDisplaySizeChanged(width, height);
        }
        if (mBeautyAdjustFilter != null) {
            mBeautyAdjustFilter.onDisplaySizeChanged(width, height);
        }

        if (mComplexionFilter != null) {
            mComplexionFilter.initFrameBuffer(width, height);
        }
        if (mBeautyBlurFilter != null) {
            mBeautyBlurFilter.initFrameBuffer((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
        if (mHighPassFilter != null) {
            mHighPassFilter.initFrameBuffer((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
        if (mHighPassBlurFilter != null) {
            mHighPassBlurFilter.initFrameBuffer((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
        if (mBeautyAdjustFilter != null) {
            mBeautyAdjustFilter.initFrameBuffer(width, height);
        }

        if (mComplexionFilter != null) {
            mComplexionFilter.setComplexionLevel(0.5f);
        }
        if (mBeautyAdjustFilter != null) {
            mBeautyAdjustFilter.setSkinBeautyIntensity(0.5f);
        }
    }

    @Override
    public int onDrawFrame(int textureId) {
        int currentTexture = textureId;
        int sourceTexture = mComplexionFilter.drawFrameBuffer(currentTexture, mVertexBuffer, mTextureBuffer);
        currentTexture = sourceTexture;

        int blurTexture = currentTexture;
        int highPassBlurTexture = currentTexture;
        // 高斯模糊
        if (mBeautyBlurFilter != null) {
            blurTexture = mBeautyBlurFilter.drawFrameBuffer(currentTexture, mVertexBuffer, mTextureBuffer);
            currentTexture = blurTexture;
        }
        // 高通滤波，做高反差保留
        if (mHighPassFilter != null) {
            mHighPassFilter.setBlurTexture(currentTexture);
            currentTexture = mHighPassFilter.drawFrameBuffer(sourceTexture, mVertexBuffer, mTextureBuffer);
        }
        // 对高反差保留的结果进行高斯模糊，过滤边沿数值
        if (mHighPassBlurFilter != null) {
            highPassBlurTexture = mHighPassBlurFilter.drawFrameBuffer(currentTexture, mVertexBuffer, mTextureBuffer);
            currentTexture = highPassBlurTexture;
        }
        // 混合处理
        if (mBeautyAdjustFilter != null) {
            currentTexture = sourceTexture;
            mBeautyAdjustFilter.setBlurTexture(blurTexture, highPassBlurTexture);
            currentTexture = mBeautyAdjustFilter.drawFrameBuffer(currentTexture, mVertexBuffer, mTextureBuffer);
        }
        return currentTexture;
    }

    @Override
    public void release() {
        super.release();
        if (mComplexionFilter != null) {
            mComplexionFilter.release();
            mComplexionFilter = null;
        }
        if (mBeautyBlurFilter != null) {
            mBeautyBlurFilter.release();
            mBeautyBlurFilter = null;
        }
        if (mHighPassFilter != null) {
            mHighPassFilter.release();
            mHighPassFilter = null;
        }
        if (mHighPassBlurFilter != null) {
            mHighPassBlurFilter.release();
            mHighPassBlurFilter = null;
        }
        if (mBeautyAdjustFilter != null) {
            mBeautyAdjustFilter.release();
            mBeautyAdjustFilter = null;
        }
    }

//    @Override
//    public void onBeauty(BeautyParam beauty) {
//        if (mComplexionFilter != null) {
//            mComplexionFilter.setComplexionLevel(beauty.complexionIntensity);
//        }
//        if (mBeautyAdjustFilter != null) {
//            mBeautyAdjustFilter.setSkinBeautyIntensity(beauty.beautyIntensity);
//        }
//        if (mBeautyFaceFilter != null) {
//            mBeautyFaceFilter.onBeauty(beauty);
//        }
//    }
}

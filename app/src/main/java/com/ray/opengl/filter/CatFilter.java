package com.ray.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.ray.opengl.R;
import com.ray.opengl.stickers.DynamicStickerLoader;
import com.ray.opengl.stickers.bean.DynamicSticker;
import com.ray.opengl.stickers.bean.DynamicStickerNormalData;
import com.ray.opengl.utils.FacePointsUtils;
import com.ray.opengl.utils.OpenGLUtils;
import com.ray.opengl.utils.TextureRotationUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.zeusee.main.hyperlandmark.jni.Face;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FUNC_ADD;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class CatFilter extends BaseFrameFilter {
    // 视椎体缩放倍数，具体数据与setLookAt 和 frustumM有关
    // 备注：setLookAt 和 frustumM 设置的结果导致了视点(eye)到近平面(near)和视点(eye)到贴纸(center)恰好是2倍的关系
    private static final float ProjectionScale = 2.0f;

    // 变换矩阵句柄
    private int mMVPMatrixHandle;

    // 贴纸变换矩阵
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    // 长宽比
    private float mRatio;

    //     贴纸坐标缓冲
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;

    // 贴纸顶点
    private float[] mStickerVertices = new float[8];

    // 贴纸数据
    protected DynamicSticker mDynamicSticker;

    // 贴纸加载器列表
    protected List<DynamicStickerLoader> mStickerLoaderList;
    private Face mFace;

    public CatFilter(Context context, DynamicSticker sticker) {
        super(context, R.raw.vertex_sticker_normal, R.raw.fragment_sticker_normal);
        mDynamicSticker = sticker;
        mStickerLoaderList = new ArrayList<>();
        mMVPMatrixHandle = glGetUniformLocation(mProgramId, "uMVPMatrix");

        // 创建贴纸加载器列表
        if (mDynamicSticker != null && mDynamicSticker.dataList != null) {
            for (int i = 0; i < mDynamicSticker.dataList.size(); i++) {
                if (mDynamicSticker.dataList.get(i) instanceof DynamicStickerNormalData) {
                    String path = mDynamicSticker.unzipPath + "/" + mDynamicSticker.dataList.get(i).stickerName;
                    mStickerLoaderList.add(new DynamicStickerLoader(this, mDynamicSticker.dataList.get(i), path));
                }
            }
        }

        initMatrix();
        initBuffer();
    }

    /**
     * 初始化纹理
     */
    private void initMatrix() {
        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    /**
     * 初始化缓冲
     */
    private void initBuffer() {
//        releaseBuffer();
        vertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        // 备注：由于后面的透视变换计算把贴纸纹理的左右反过来了，这里用纹理坐标做个纠正
        textureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices_flipx);
    }

    @Override
    public void onReady(int width, int height) {
        super.onReady(width, height);
        mRatio = (float) imgWidth / imgHeight;
        //投影转换矩阵
        Matrix.frustumM(mProjectionMatrix, 0, -mRatio, mRatio, -1.0f, 1.0f, 6.0f, 18.0f);
        //视见转换矩阵
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }


    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        if (mMVPMatrixHandle != OpenGLUtils.GL_NOT_INIT) {
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        }
        // 绘制到FBO中，需要开启混合模式
        GLES20.glEnable(GL_BLEND);
        GLES20.glBlendEquation(GL_FUNC_ADD);
        GLES20.glBlendFuncSeparate(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE, GLES20.GL_ONE);
    }

    @Override
    public int onDrawFrame(int textureID) {
        if (null == mFace) {
            return textureID; // 如果当前一帧画面没有检测到人脸信息，什么事情都不用做
        }
        Matrix.setIdentityM(mMVPMatrix, 0);
        // 1：设置视窗  紫色区域 屏幕的宽和高 视图大小规定
        glViewport(0, 0, mWidth, mHeight);
        // 这里是因为要渲染到FBO缓存中，而不是直接显示到屏幕上
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);

        // 2：使用着色器程序
        glUseProgram(mProgramId);

        // 渲染 传值
        // 1：顶点数据
        mVertexBuffer.position(0);
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer); // 传值
        glEnableVertexAttribArray(vPosition); // 传值后激活

        // 2：纹理坐标
        mTextureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer); // 传值
        glEnableVertexAttribArray(vCoord); // 传值后激活

        // 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层
        glBindTexture(GL_TEXTURE_2D, textureID); // 绑定
        glUniform1i(vTexture, 0); // 传递参数

        onDrawFrameBegin();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知opengl绘制

        // 解绑fbo
        glDisableVertexAttribArray(vPosition);
        glDisableVertexAttribArray(vCoord);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_BLEND);
        return drawSticker(textureID);
    }


    public int drawSticker(int textureID) {
        // 2、将贴纸逐个绘制到FBO中
        if (mStickerLoaderList.size() > 0 && mFace != null) {
            // 逐个人脸绘制
            for (int stickerIndex = 0; stickerIndex < mStickerLoaderList.size(); stickerIndex++) {
                synchronized (this) {
                    mStickerLoaderList.get(stickerIndex).updateStickerTexture();
                    calculateStickerVertices((DynamicStickerNormalData) mStickerLoaderList.get(stickerIndex).getStickerData(),
                            mFace);
                    // 1：设置视窗  紫色区域 屏幕的宽和高 视图大小规定
                    glViewport(0, 0, mWidth, mHeight);
                    // 这里是因为要渲染到FBO缓存中，而不是直接显示到屏幕上
                    glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);

                    // 2：使用着色器程序
                    glUseProgram(mProgramId);

                    // 渲染 传值
                    // 1：顶点数据
                    vertexBuffer.position(0);
                    glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer); // 传值
                    glEnableVertexAttribArray(vPosition); // 传值后激活

                    // 2：纹理坐标
                    textureBuffer.position(0);
                    glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer); // 传值
                    glEnableVertexAttribArray(vCoord); // 传值后激活

                    // 片元 vTexture
                    glActiveTexture(GL_TEXTURE0); // 激活图层
                    glBindTexture(GL_TEXTURE_2D, mStickerLoaderList.get(stickerIndex).getStickerTexture()); // 绑定
                    glUniform1i(vTexture, 0); // 传递参数
                    onDrawFrameBegin();
                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知opengl绘制

                    glDisableVertexAttribArray(vPosition);
                    glDisableVertexAttribArray(vCoord);
                    // 解绑fbo
                    glBindTexture(GL_TEXTURE_2D, 0);
                    glBindFramebuffer(GL_FRAMEBUFFER, 0);
                    glDisable(GL_BLEND);
//                    glUseProgram(0);
//                    super.drawFrameBuffer(mStickerLoaderList.get(stickerIndex).getStickerTexture(), mVertexBuffer, mTextureBuffer);
                }
            }
            GLES20.glFlush();
        }
        return mFrameBufferTextures[0];
    }


    /**
     * 更新贴纸顶点
     * TODO 待优化的点：消除姿态角误差、姿态角给贴纸偏移量造成的误差
     *
     * @param stickerData
     */
    private void calculateStickerVertices(DynamicStickerNormalData stickerData, Face oneFace) {
        if (oneFace == null || oneFace.landmarks == null) {
            return;
        }
        // 步骤一、计算贴纸的中心点和顶点坐标
        // 备注：由于frustumM设置的bottom 和top 为 -1.0 和 1.0，这里为了方便计算，直接用高度作为基准值来计算
        // 1.1、计算贴纸相对于人脸的宽高
        float stickerWidth = (float) FacePointsUtils.getDistance(
                oneFace.landmarks[stickerData.startIndex * 2],
                oneFace.landmarks[stickerData.startIndex * 2 + 1],
                oneFace.landmarks[stickerData.endIndex * 2],
                oneFace.landmarks[stickerData.endIndex * 2 + 1]) * stickerData.baseScale;
        float stickerHeight = stickerWidth * (float) stickerData.height / (float) stickerData.width;

        // 1.2、根据贴纸的参数计算出中心点的坐标
        float centerX = 0.0f;
        float centerY = 0.0f;
        for (int i = 0; i < stickerData.centerIndexList.length; i++) {
            centerX += (oneFace.landmarks[stickerData.centerIndexList[i] * 2]);
            centerY += (oneFace.landmarks[stickerData.centerIndexList[i] * 2 + 1]);
        }
        centerX /= (float) stickerData.centerIndexList.length;
        centerY /= (float) stickerData.centerIndexList.length;
        //imgHeight为大的那个
        centerX = centerX / imgHeight * ProjectionScale;
        centerY = centerY / imgHeight * ProjectionScale;
        // 1.3、求出真正的中心点顶点坐标，这里由于frustumM设置了长宽比，因此ndc坐标计算时需要变成mRatio:1，这里需要转换一下
        float ndcCenterX = (centerX - mRatio) * ProjectionScale;
        float ndcCenterY = (centerY - 1.0f) * ProjectionScale;

        // 1.4、贴纸的宽高在ndc坐标系中的长度
        float ndcStickerWidth = stickerWidth / imgHeight * ProjectionScale;
        float ndcStickerHeight = ndcStickerWidth * (float) stickerData.height / (float) stickerData.width;

        // 1.5、根据贴纸参数求偏移的ndc坐标
        float offsetX = (stickerWidth * stickerData.offsetX) / imgHeight * ProjectionScale;
        float offsetY = (stickerHeight * stickerData.offsetY) / imgHeight * ProjectionScale;

        // 1.6、贴纸带偏移量的锚点的ndc坐标，即实际贴纸的中心点在OpenGL的顶点坐标系中的位置
        float anchorX = ndcCenterX + offsetX * ProjectionScale;
        float anchorY = ndcCenterY + offsetY * ProjectionScale;

        // 1.7、根据前面的锚点，计算出贴纸实际的顶点坐标
        mStickerVertices[0] = anchorX - ndcStickerWidth;
        mStickerVertices[1] = anchorY - ndcStickerHeight;
        mStickerVertices[2] = anchorX + ndcStickerWidth;
        mStickerVertices[3] = anchorY - ndcStickerHeight;
        mStickerVertices[4] = anchorX - ndcStickerWidth;
        mStickerVertices[5] = anchorY + ndcStickerHeight;
        mStickerVertices[6] = anchorX + ndcStickerWidth;
        mStickerVertices[7] = anchorY + ndcStickerHeight;

        vertexBuffer.clear();
        vertexBuffer.position(0);
        vertexBuffer.put(mStickerVertices);

        // 步骤二、根据人脸姿态角计算透视变换的总变换矩阵
        // 2.1、将Z轴平移到贴纸中心点，因为贴纸模型矩阵需要做姿态角变换
        // 平移主要是防止贴纸变形
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, ndcCenterX, ndcCenterY, 0);

        // 2.2、贴纸姿态角旋转
        // TODO 人脸关键点给回来的pitch角度似乎不太对？？SDK给过来的pitch角度值太小了，比如抬头低头pitch的实际角度30度了，SDK返回的结果才十几度，后续再看看如何优化
        // 3,-10.938059-4.3219891.6387599
//        float pitchAngle = -(float) (oneFace.eulerAngles[0] * 180f / Math.PI);
//        float yawAngle = (float) (oneFace.eulerAngles[1] * 180f / Math.PI);
//        float rollAngle = (float) (oneFace.eulerAngles[2] * 180f / Math.PI);
        Log.v("欧拉角:", "pitchAngle:" + oneFace.eulerAngles[0] + "yawAngle:" + oneFace.eulerAngles[1] + "rollAngle:" + oneFace.eulerAngles[2]);
        float pitchAngle = -(float) (oneFace.eulerAngles[0]);
        float yawAngle = (float) (oneFace.eulerAngles[1]);
        float rollAngle = (float) (oneFace.eulerAngles[2]);

        // 限定左右扭头幅度不超过50°，销毁人脸关键点SDK带来的偏差
        if (Math.abs(yawAngle) > 50) {
            yawAngle = (yawAngle / Math.abs(yawAngle)) * 50;
        }
        // 限定抬头低头最大角度，消除人脸关键点SDK带来的偏差
        if (Math.abs(pitchAngle) > 30) {
            pitchAngle = (pitchAngle / Math.abs(pitchAngle)) * 30;
        }
        // 贴纸姿态角变换，优先z轴变换，消除手机旋转的角度影响，否则会导致扭头、抬头、低头时贴纸变形的情况
        Matrix.rotateM(mModelMatrix, 0, rollAngle, 0, 0, 1);
        Matrix.rotateM(mModelMatrix, 0, yawAngle, 0, 1, 0);
        Matrix.rotateM(mModelMatrix, 0, pitchAngle, 1, 0, 0);

        // 2.4、将Z轴平移回到原来构建的视椎体的位置，即需要将坐标z轴平移回到屏幕中心，此时才是贴纸的实际模型矩阵
        Matrix.translateM(mModelMatrix, 0, -ndcCenterX, -ndcCenterY, 0);

        // 2.5、计算总变换矩阵。MVPMatrix 的矩阵计算是 MVPMatrix = ProjectionMatrix * ViewMatrix * ModelMatrix
        // 备注：矩阵相乘的顺序不同得到的结果是不一样的，不同的顺序会导致前面计算过程不一致，这点希望大家要注意
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mModelMatrix, 0);
    }

    public void setFace(Face face) {
        this.mFace = face;
    }


    /**
     * 释放缓冲
     */
    private void releaseBuffer() {
        if (mVertexBuffer != null) {
            mVertexBuffer.clear();
            mVertexBuffer = null;
        }
        if (mTextureBuffer != null) {
            mTextureBuffer.clear();
            mTextureBuffer = null;
        }
    }
}

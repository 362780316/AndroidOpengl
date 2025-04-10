package com.ray.opengl.filter;

import android.content.Context;
import com.ray.opengl.R;
import com.ray.opengl.base.GLImageGaussianBlurFilter;
import com.ray.opengl.utils.OpenGLUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.zeusee.main.hyperlandmark.jni.Face;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

// 亮眼
public class BrightEyeFilter extends BaseFrameFilter {
    public static final String TAG = BrightEyeFilter.class.getSimpleName();
    // 顶点坐标数组最大长度，这里主要用于复用缓冲
    private static final int MaxLength = 100;
    private float[] vertexPoints = new float[MaxLength];

    int u_blurTexture;
    int u_maskTexture;
    int u_strength;
    int u_enableProcess;

    private FloatBuffer mBrightEyeVertexBuffer;
    private FloatBuffer mMaskTextureBuffer;
    protected ShortBuffer mIndexBuffer;
    private Face mFace;
    // 索引长度
    protected int mIndexLength;
    private int mEyeMaskTexture;

    // 用于高斯模糊处理
//    private GLImageGaussianBlurFilter mBlurFilter;
    private GLImageGaussianBlurFilter mBlurNextFilter;

    // 高斯模糊纹理
    private int mBlurTexture = OpenGLUtils.GL_NOT_TEXTURE;
    private int mBlurTexture2 = OpenGLUtils.GL_NOT_TEXTURE;

    Context context;
    /**
     * 眼睛遮罩纹理坐标
     */
    private static final float[] mEyeMaskTextureVertices = new float[]{
            0.102757f, 0.465517f,
            0.175439f, 0.301724f,
            0.370927f, 0.310345f,
            0.446115f, 0.603448f,
            0.353383f, 0.732759f,
            0.197995f, 0.689655f,

            0.566416f, 0.629310f,
            0.659148f, 0.336207f,
            0.802005f, 0.318966f,
            0.884712f, 0.465517f,
            0.812030f, 0.681034f,
            0.681704f, 0.750023f,

            0.273183f, 0.241379f,
            0.275689f, 0.758620f,

            0.721805f, 0.275862f,
            0.739348f, 0.758621f,
    };

    /**
     * 索引，glDrawElements使用
     */
    public static final short[] Indices = {
            0, 1, 2,
            2, 1, 3,
    };

    /**
     * 眼睛部分索引
     */
    private static final short[] mEyeIndices = new short[]{
            0, 5, 1,
            1, 5, 12,
            12, 5, 13,
            12, 13, 4,
            12, 4, 2,
            2, 4, 3,

            6, 7, 11,
            7, 11, 14,
            14, 11, 15,
            14, 15, 10,
            14, 10, 8,
            8, 10, 9
    };

    public BrightEyeFilter(Context context) {
        super(context, R.raw.brighteye_vertex, R.raw.brighteye_fragment);
//        mBlurFilter = new GLImageGaussianBlurFilter(context);
//        mBlurFilter.setBlurSize(1.0f);
        this.context = context;
        mBlurNextFilter = new GLImageGaussianBlurFilter(context);
        mBlurNextFilter.setBlurSize(3.0f);

        mEyeMaskTexture = OpenGLUtils.createTextureFromAssets(context, "texture/makeup_eye_mask.png");
        u_blurTexture = glGetUniformLocation(mProgramId, "blurTexture"); // 经过高斯模糊处理的图像纹理
        u_maskTexture = glGetUniformLocation(mProgramId, "maskTexture"); // 眼睛遮罩图像纹理
        u_strength = glGetUniformLocation(mProgramId, "strength"); // 明亮程度
        u_enableProcess = glGetUniformLocation(mProgramId, "enableProcess"); // 是否允许亮眼，没有人脸时不需要亮眼处理

        mBrightEyeVertexBuffer = ByteBuffer.allocateDirect(MaxLength * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBrightEyeVertexBuffer.position(0);

        mMaskTextureBuffer = ByteBuffer.allocateDirect(MaxLength * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMaskTextureBuffer.position(0);

        mIndexBuffer = ByteBuffer.allocateDirect(MaxLength * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndexBuffer.position(0);
    }

    @Override
    public void onReady(int width, int height) {
        super.onReady(width, height);
//        mBlurFilter.onInputSizeChanged(imgWidth, imgHeight);
//        mBlurFilter.onDisplaySizeChanged(width, height);
//        mBlurFilter.initFrameBuffer(imgWidth, imgHeight);
        mBlurNextFilter.onInputSizeChanged(imgWidth, imgHeight);
//        mBlurNextFilter.onInputSizeChanged((int) (imgWidth / 3.0f), (int) (imgHeight / 3.0f));
        mBlurNextFilter.onDisplaySizeChanged(width, height);
        mBlurNextFilter.initFrameBuffer(width, height);
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        if (mBlurTexture != -1) {
            OpenGLUtils.bindTexture(u_blurTexture, mBlurTexture, 1);
        }
        if (mEyeMaskTexture != -1) {
            OpenGLUtils.bindTexture(u_maskTexture, mEyeMaskTexture, 2);
        }
    }

    @Override
    public int onDrawFrame(int textureID) {
        if (null == mFace) {
            return textureID;
        }
        // 1：设置视窗
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

        mIndexBuffer.clear();
        mIndexBuffer.put(Indices);
        mIndexBuffer.position(0);
        mIndexLength = Indices.length;

        //一
        glDrawElements(GL_TRIANGLES, mIndexLength, GL_UNSIGNED_SHORT, mIndexBuffer);// 通知opengl绘制
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        //二
        mBlurTexture = mBlurNextFilter.drawFrameBuffer(textureID, mVertexBuffer, mTextureBuffer);
//        mBlurTexture2 = mBlurNextFilter.drawFrameBuffer(textureID, mVertexBuffer, mTextureBuffer);
        //三
        drawBrightEye(textureID);
        return mFrameBufferTextures[0];//返回fbo的纹理id
    }

    private void updateBuffer() {
        getBrightEyeVertices();
        mBrightEyeVertexBuffer.clear();
        mBrightEyeVertexBuffer.put(vertexPoints);
        mBrightEyeVertexBuffer.position(0);

        // 更新眼睛遮罩纹理坐标
        mMaskTextureBuffer.clear();
        mMaskTextureBuffer.put(mEyeMaskTextureVertices);
        mMaskTextureBuffer.position(0);
        // 更新眼睛索引
        mIndexBuffer.clear();
        mIndexBuffer.put(mEyeIndices);
        mIndexBuffer.position(0);
        mIndexLength = mEyeIndices.length;
    }

    private void drawBrightEye(int textureID) {
        mIndexLength = mEyeIndices.length;
        glViewport(0, 0, mWidth, mHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        // 2：使用着色器程序
        glUseProgram(mProgramId);
        updateBuffer();
        mBrightEyeVertexBuffer.position(0);
        glVertexAttribPointer(vPosition, 2,
                GL_FLOAT, false, 0, mBrightEyeVertexBuffer);
        glEnableVertexAttribArray(vPosition);

        // 绑定纹理坐标缓冲
        mMaskTextureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2,
                GL_FLOAT, false, 0, mMaskTextureBuffer);
        glEnableVertexAttribArray(vCoord);

//        setFloat(u_strength, 0.8f);
//        setInteger(u_enableProcess, 1);
        glUniform1f(u_strength, 1.0f);
        glUniform1i(u_enableProcess, 1);

        // 绑定纹理
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glUniform1i(vTexture, 0);
        onDrawFrameBegin();
        glDrawElements(GL_TRIANGLES, mIndexLength, GL_UNSIGNED_SHORT, mIndexBuffer);// 通知opengl绘制

        // 解绑fbo
        glDisableVertexAttribArray(vPosition);
        glDisableVertexAttribArray(vCoord);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glUseProgram(0);
    }

    /**
     * 取得亮眼需要的顶点坐标
     */
    public synchronized void getBrightEyeVertices() {
        if (vertexPoints == null || vertexPoints.length < 32
                || mFace == null) {
            return;
        }

        int[] landmarks = mFace.landmarks; // 传 mFace 眼睛坐标 给着色器
        vertexPoints[0 * 2] = view2openglX(landmarks[94 * 2]);
        vertexPoints[0 * 2 + 1] = view2openglY(landmarks[94 * 2 + 1]);

        //2
        vertexPoints[1 * 2] = view2openglX(landmarks[1 * 2]);
        vertexPoints[1 * 2 + 1] = view2openglY(landmarks[1 * 2 + 1]);

        //54
        vertexPoints[2 * 2] = view2openglX(landmarks[53 * 2]);
        vertexPoints[2 * 2 + 1] = view2openglY(landmarks[53 * 2 + 1]);

        //60
        vertexPoints[3 * 2] = view2openglX(landmarks[59 * 2]);
        vertexPoints[3 * 2 + 1] = view2openglY(landmarks[59 * 2 + 1]);

        //68
        vertexPoints[4 * 2] = view2openglX(landmarks[67 * 2]);
        vertexPoints[4 * 2 + 1] = view2openglY(landmarks[67 * 2 + 1]);

        //13
        vertexPoints[5 * 2] = view2openglX(landmarks[12 * 2]);
        vertexPoints[5 * 2 + 1] = view2openglY(landmarks[12 * 2 + 1]);

        //28
        vertexPoints[6 * 2] = view2openglX(landmarks[27 * 2]);
        vertexPoints[6 * 2 + 1] = view2openglY(landmarks[27 * 2 + 1]);

        //105
        vertexPoints[7 * 2] = view2openglX(landmarks[104 * 2]);
        vertexPoints[7 * 2 + 1] = view2openglY(landmarks[104 * 2 + 1]);

        //86
        vertexPoints[8 * 2] = view2openglX(landmarks[85 * 2]);
        vertexPoints[8 * 2 + 1] = view2openglY(landmarks[85 * 2 + 1]);

        //21
        vertexPoints[9 * 2] = view2openglX(landmarks[20 * 2]);
        vertexPoints[9 * 2 + 1] = view2openglY(landmarks[20 * 2 + 1]);

        //48
        vertexPoints[10 * 2] = view2openglX(landmarks[47 * 2]);
        vertexPoints[10 * 2 + 1] = view2openglY(landmarks[47 * 2 + 1]);

        //52
        vertexPoints[11 * 2] = view2openglX(landmarks[51 * 2]);
        vertexPoints[11 * 2 + 1] = view2openglY(landmarks[51 * 2 + 1]);

        //35
        vertexPoints[12 * 2] = view2openglX(landmarks[34 * 2]);
        vertexPoints[12 * 2 + 1] = view2openglY(landmarks[34 * 2 + 1]);

        //4
        vertexPoints[13 * 2] = view2openglX(landmarks[3 * 2]);
        vertexPoints[13 * 2 + 1] = view2openglY(landmarks[3 * 2 + 1]);

        //42
        vertexPoints[14 * 2] = view2openglX(landmarks[41 * 2]);
        vertexPoints[14 * 2 + 1] = view2openglY(landmarks[41 * 2 + 1]);

        //44
        vertexPoints[15 * 2] = view2openglX(landmarks[43 * 2]);
        vertexPoints[15 * 2 + 1] = view2openglY(landmarks[43 * 2 + 1]);
//        for (int i = 0; i < 16; i++) {
//            Log.v(TAG, "眼睛的16个坐标" + vertexPoints[i]);
//        }
    }


    public void setFace(Face mFace) { // C++层把人脸最终5关键点成果的(mFaceTrack.getFace()) 赋值给此函数
        this.mFace = mFace;
    }

    private float view2openglX(int x) {
        float centerX = imgWidth / 2.0f;
        float t = x - centerX;
        return (t / centerX);
    }

    private float view2openglY(int y) {
        float centerY = imgHeight / 2.0f;
        float s = centerY - y;
        return -(s / centerY);
    }
}

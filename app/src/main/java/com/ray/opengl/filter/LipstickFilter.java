package com.ray.opengl.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.ray.opengl.R;
import com.ray.opengl.makeup.bean.DynamicMakeup;
import com.ray.opengl.makeup.bean.MakeupBaseData;
import com.ray.opengl.makeup.bean.MakeupLipstickData;
import com.ray.opengl.makeup.bean.MakeupNormaData;
import com.ray.opengl.resource.ResourceCodec;
import com.ray.opengl.resource.ResourceDataCodec;
import com.ray.opengl.utils.BufferHelper;
import com.ray.opengl.utils.OpenGLUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.zeusee.main.hyperlandmark.jni.Face;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

// 口红
public class LipstickFilter extends BaseFrameFilter {
    public static final String TAG = LipstickFilter.class.getSimpleName();
    // 顶点坐标数组最大长度，这里主要用于复用缓冲
    private float[] vertexPoints = new float[40];
    int u_materialTexture;
    int u_maskTexture;
    int u_strength;
    int u_makeupType;

    private FloatBuffer mBrightEyeVertexBuffer;
    private FloatBuffer mMaskTextureBuffer;
    protected ShortBuffer mIndexBuffer;
    private Face mFace;
    // 索引长度
    protected int mIndexLength;
    private int mEyeMaskTexture;
    private int mMaterialTexture;

    private static final String MakeupDirectory = "Makeup";

    /**
     * 嘴唇
     */
    private static final float[] mEyeMaskTextureVertices = new float[]{
            0.171821f, 0.409089f,
            0.281787f, 0.318381f,
            0.398625f, 0.272727f,
            0.515464f, 0.300303f,
            0.618557f, 0.287279f,
            0.728522f, 0.333333f,
            0.845361f, 0.424242f,
            0.776632f, 0.575758f,
            0.687285f, 0.742424f,
            0.515464f, 0.833333f,
            0.357388f, 0.757576f,
            0.274914f, 0.651515f,
            0.226804f, 0.424242f,
            0.378007f, 0.439393f,
            0.508592f, 0.469696f,
            0.680413f, 0.454545f,
            0.776632f, 0.439393f,
            0.646048f, 0.575757f,
            0.501718f, 0.590909f,
            0.336769f, 0.515152f,
    };

    /**
     * 索引，glDrawElements使用
     */
    public static final short[] Indices = {
            0, 1, 2,
            2, 1, 3,
    };

    /**
     * 索引，glDrawElements使用
     */
    public static final short[] mLipIndices = {
            // 上嘴唇部分
            0, 1, 12,
            12, 1, 13,
            13, 1, 2,
            2, 13, 14,
            2, 14, 3,
            3, 14, 4,
            4, 14, 15,
            4, 15, 5,
            5, 15, 16,
            5, 16, 6,
            // 下嘴唇部分
            6, 16, 7,
            16, 7, 17,
            17, 7, 8,
            17, 8, 18,
            18, 8, 9,
            18, 9, 10,
            18, 10, 19,
            19, 10, 11,
            19, 11, 12,
            12, 11, 0,
    };

    public LipstickFilter(Context context, DynamicMakeup dynamicMakeup) {
        super(context, R.raw.lipstick_vertex, R.raw.lipstick_fragment);
        mEyeMaskTexture = OpenGLUtils.createTextureFromAssets(context, "texture/makeup_lips_mask.png");
        u_materialTexture = glGetUniformLocation(mProgramId, "materialTexture");
        u_maskTexture = glGetUniformLocation(mProgramId, "maskTexture"); // 眼睛遮罩图像纹理
        u_strength = glGetUniformLocation(mProgramId, "strength"); // 明亮程度
        u_makeupType = glGetUniformLocation(mProgramId, "makeupType"); // 是否允许亮眼，没有人脸时不需要亮眼处理
        mMaskTextureBuffer = BufferHelper.getFloatBuffer(mEyeMaskTextureVertices);
        mIndexBuffer = BufferHelper.getShortBuffer(mLipIndices);
//        mIndexBuffer = ByteBuffer.allocateDirect(100 * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
//        mIndexBuffer.position(0);
        loadMaterialTexture(dynamicMakeup);
    }

    ResourceDataCodec mResourceCodec;

    protected void loadMaterialTexture(DynamicMakeup dynamicMakeup) {
        String unzipPath = dynamicMakeup.unzipPath;
        // 彩妆数据
        MakeupBaseData mMakeupData = dynamicMakeup.makeupList.get(0);
        Pair pair = ResourceCodec.getResourceFile(unzipPath);
        if (pair != null) {
            mResourceCodec = new ResourceDataCodec(unzipPath + "/" + (String) pair.first, unzipPath + "/" + pair.second);
        }
        if (mResourceCodec != null) {
            try {
                mResourceCodec.init();
            } catch (IOException e) {
                Log.e(TAG, "loadMaterialTexture: ", e);
                mResourceCodec = null;
            }
        }

        // 如果是唇彩，则加载lookupTable 数据，否则加载普通素材数据
        Bitmap bitmap = null;
        if (mMakeupData.makeupType.getName().equals("lipstick")) {
            bitmap = mResourceCodec.loadBitmap(((MakeupLipstickData) mMakeupData).lookupTable);
        } else if (((MakeupNormaData) mMakeupData).materialData != null) {
            bitmap = mResourceCodec.loadBitmap(((MakeupNormaData) mMakeupData).materialData.name);
        }

        // 判断是否取得素材或者lut纹理图片
        if (bitmap != null) {
            if (mMaterialTexture != OpenGLUtils.GL_NOT_TEXTURE) {
                glDeleteTextures(1, new int[]{mMaterialTexture}, 0);
                mMaterialTexture = OpenGLUtils.GL_NOT_TEXTURE;
            }
            mMaterialTexture = OpenGLUtils.createTexture(bitmap);
            bitmap.recycle();
        } else {
            mMaterialTexture = OpenGLUtils.GL_NOT_TEXTURE;
        }
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        // 绑定素材纹理，素材纹理可能不存在，不存在时不需要绑定
        if (mMaterialTexture != OpenGLUtils.GL_NOT_TEXTURE) {
            OpenGLUtils.bindTexture(u_materialTexture, mMaterialTexture, 1);
        }

        if (mEyeMaskTexture != OpenGLUtils.GL_NOT_TEXTURE) {
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

        glUniform1i(u_makeupType, 0);

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
        drawMakeup(textureID);
        return mFrameBufferTextures[0];//返回fbo的纹理id
    }

    private void updateBuffer() {
        getLipVertices();
        mBrightEyeVertexBuffer = BufferHelper.getFloatBuffer(vertexPoints);
        mIndexBuffer.clear();
        mIndexBuffer.put(mLipIndices);
        mIndexBuffer.position(0);
        mIndexLength = mLipIndices.length;
    }

    private void drawMakeup(int textureID) {
        glViewport(0, 0, mWidth, mHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        // 2：使用着色器程序
        glUseProgram(mProgramId);
        // 使能混合功能
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
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
        glUniform1f(u_strength, 0.7f);
        glUniform1i(u_makeupType, 1);

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
        glDisable(GL_BLEND);
        glUseProgram(0);
    }

    /**
     * 取得亮眼需要的顶点坐标
     */
    public synchronized void getLipVertices() {
        if (vertexPoints == null || vertexPoints.length < 40
                || mFace == null) {
            return;
        }
        //46
        int[] landmarks = mFace.landmarks; // 传 mFace 眼睛坐标 给着色器
        vertexPoints[0 * 2] = view2openglX(landmarks[45 * 2]);
        vertexPoints[0 * 2 + 1] = view2openglY(landmarks[45 * 2 + 1]);

        //38
        vertexPoints[1 * 2] = view2openglX(landmarks[37 * 2]);
        vertexPoints[1 * 2 + 1] = view2openglY(landmarks[37 * 2 + 1]);

        //40
        vertexPoints[2 * 2] = view2openglX(landmarks[39 * 2]);
        vertexPoints[2 * 2 + 1] = view2openglY(landmarks[39 * 2 + 1]);

        //39
        vertexPoints[3 * 2] = view2openglX(landmarks[38 * 2]);
        vertexPoints[3 * 2 + 1] = view2openglY(landmarks[38 * 2 + 1]);

        //27
        vertexPoints[4 * 2] = view2openglX(landmarks[26 * 2]);
        vertexPoints[4 * 2 + 1] = view2openglY(landmarks[26 * 2 + 1]);

        //34
        vertexPoints[5 * 2] = view2openglX(landmarks[33 * 2]);
        vertexPoints[5 * 2 + 1] = view2openglY(landmarks[33 * 2 + 1]);

        //51
        vertexPoints[6 * 2] = view2openglX(landmarks[50 * 2]);
        vertexPoints[6 * 2 + 1] = view2openglY(landmarks[50 * 2 + 1]);

        //5
        vertexPoints[7 * 2] = view2openglX(landmarks[4 * 2]);
        vertexPoints[7 * 2 + 1] = view2openglY(landmarks[4 * 2 + 1]);

        //31
        vertexPoints[8 * 2] = view2openglX(landmarks[30 * 2]);
        vertexPoints[8 * 2 + 1] = view2openglY(landmarks[30 * 2 + 1]);

        //33
        vertexPoints[9 * 2] = view2openglX(landmarks[32 * 2]);
        vertexPoints[9 * 2 + 1] = view2openglY(landmarks[32 * 2 + 1]);

        //65
        vertexPoints[10 * 2] = view2openglX(landmarks[64 * 2]);
        vertexPoints[10 * 2 + 1] = view2openglY(landmarks[64 * 2 + 1]);

        //66
        vertexPoints[11 * 2] = view2openglX(landmarks[65 * 2]);
        vertexPoints[11 * 2 + 1] = view2openglY(landmarks[65 * 2 + 1]);
        //----------------------------------
        //62
        vertexPoints[12 * 2] = view2openglX(landmarks[61 * 2]);
        vertexPoints[12 * 2 + 1] = view2openglY(landmarks[61 * 2 + 1]);
        //41
        vertexPoints[13 * 2] = view2openglX(landmarks[40 * 2]);
        vertexPoints[13 * 2 + 1] = view2openglY(landmarks[40 * 2 + 1]);
        //37
        vertexPoints[14 * 2] = view2openglX(landmarks[36 * 2]);
        vertexPoints[14 * 2 + 1] = view2openglY(landmarks[36 * 2 + 1]);
        //26
        vertexPoints[15 * 2] = view2openglX(landmarks[25 * 2]);
        vertexPoints[15 * 2 + 1] = view2openglY(landmarks[25 * 2 + 1]);
        //下嘴唇
        //43
        vertexPoints[16 * 2] = view2openglX(landmarks[42 * 2]);
        vertexPoints[16 * 2 + 1] = view2openglY(landmarks[42 * 2 + 1]);
        //3
        vertexPoints[17 * 2] = view2openglX(landmarks[2 * 2]);
        vertexPoints[17 * 2 + 1] = view2openglY(landmarks[2 * 2 + 1]);
        //104
        vertexPoints[18 * 2] = view2openglX(landmarks[103 * 2]);
        vertexPoints[18 * 2 + 1] = view2openglY(landmarks[103 * 2 + 1]);
        //64
        vertexPoints[19 * 2] = view2openglX(landmarks[63 * 2]);
        vertexPoints[19 * 2 + 1] = view2openglY(landmarks[63 * 2 + 1]);
//        for (int i = 0; i < 20; i++) {
//            Log.v(TAG, "嘴唇的20个坐标" + vertexPoints[i]);
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

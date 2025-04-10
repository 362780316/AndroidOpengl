package com.ray.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.ray.opengl.R;
import com.ray.opengl.utils.ShaderHelper;
import com.ray.opengl.utils.TextResourceReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.zeusee.main.hyperlandmark.jni.Face;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class DemoFilter extends BaseFrameFilter {
    private Face mFace; // 人脸追踪+人脸5关键点 最终的成果

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private int bufferLength = 106 * 2 * 4;
    private int programId = -1;
    private int aPositionHandle;
    private int fPosition;
    private int vboId;

    int textureId;
    Context context;

    private int[] vertexBuffers;//vboID

    public DemoFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.base_fragment);
        this.context = context;
    }

    public DemoFilter(Context context, float[] vertexArray) {
        super(context, R.raw.demo_vertex, R.raw.demo_fragment, vertexArray);
    }


    @Override
    public void onReady(int width, int height) { // 自定义渲染器来更改更新信息
        super.onReady(width, height);
    }

    @Override
    public int onDrawFrame(int textureID) {
        if (null == mFace) {
            return textureID; // 如果当前一帧画面没有检测到人脸信息，什么事情都不用做
        }
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

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知opengl绘制

        // 解绑fbo
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        drawPoint();
        return mFrameBuffers[0];//返回vbo的纹理id
    }

    private void drawPoint() {
        GLES20.glUseProgram(programId);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffers[0]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, bufferLength, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false,
                0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 106);
    }

    public void setFace(Face mFace) { // C++层把人脸最终5关键点成果的(mFaceTrack.getFace()) 赋值给此函数
        this.mFace = mFace;

        vertexBuffer = ByteBuffer.allocateDirect(bufferLength)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        ;
        vertexBuffer.rewind();
        vertexBuffer.put(mFace.glLandmarks);
        vertexBuffer.position(0);
        String vertexSource = TextResourceReader.readTextFileFromResource(context, R.raw.demo_vertex);
        String fragmentSource = TextResourceReader.readTextFileFromResource(context,
                R.raw.demo_fragment);
        int vertexShaderId = ShaderHelper.compileVertexShader(vertexSource);
        int fragmentShaderId = ShaderHelper.compileFragmentShader(fragmentSource);
        programId = ShaderHelper.linkProgram(vertexShaderId, fragmentShaderId);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");

        vertexBuffers = new int[1];
        GLES20.glGenBuffers(1, vertexBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bufferLength, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}

package com.ray.opengl.filter;

import android.content.Context;
import android.graphics.PointF;

import com.ray.opengl.utils.ShaderHelper;
import com.ray.opengl.utils.BufferHelper;
import com.ray.opengl.utils.TextResourceReader;

import java.nio.FloatBuffer;
import java.util.LinkedList;

import static android.opengl.GLES20.*;

public class BaseFilter {
    private int mVertexSourceId;
    private int mFragmentSourceId;

    protected FloatBuffer mVertexBuffer;//顶点坐标数据缓冲区
    protected FloatBuffer mTextureBuffer;//纹理坐标数据缓冲区

    protected int mProgramId;
    protected int vPosition;
    protected int vCoord;
    protected int vMatrix;
    protected int vTexture;
    protected int mWidth;
    protected int mHeight;

    private LinkedList<Runnable> mRunOnDraw;
    protected Context mContext;

    public BaseFilter(Context context, int vertexSourceId, int fragmentSourceId) {
        this.mContext = context;
        this.mVertexSourceId = vertexSourceId;
        this.mFragmentSourceId = fragmentSourceId;
        mRunOnDraw = new LinkedList<>();

        float[] VERTEX = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
        };
        mVertexBuffer = BufferHelper.getFloatBuffer(VERTEX);
//        float[] TEXTURE = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,};
        float[] TEXTURE = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };
        mTextureBuffer = BufferHelper.getFloatBuffer(TEXTURE);
        init(context);
        changeTextureData();
    }


    public BaseFilter(Context context, int vertexSourceId, int fragmentSourceId, float[] vertexArray) {
        this.mVertexSourceId = vertexSourceId;
        this.mFragmentSourceId = fragmentSourceId;
        mVertexBuffer = BufferHelper.getFloatBuffer(vertexArray);
        // float[] TEXTURE = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,};
        float[] TEXTURE = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };
        mTextureBuffer = BufferHelper.getFloatBuffer(TEXTURE);
        init(context);
        changeTextureData();
    }


    /**
     * 修改纹理坐标 textureData（有需求可以重写该方法）
     */
    protected void changeTextureData() {

    }

    private void init(Context context) {
        if (mVertexSourceId == 0) {
            return;
        }
        String vertexSource = TextResourceReader.readTextFileFromResource(context, mVertexSourceId);
        String fragmentSource = TextResourceReader.readTextFileFromResource(context,
                mFragmentSourceId);
        int vertexShaderId = ShaderHelper.compileVertexShader(vertexSource);
        int fragmentShaderId = ShaderHelper.compileFragmentShader(fragmentSource);

        mProgramId = ShaderHelper.linkProgram(vertexShaderId, fragmentShaderId);
        // TODO 得到程序了，着色器可以释放了
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        //        ShaderHelper.validateProgram(mProgramId);
        vPosition = glGetAttribLocation(mProgramId, "vPosition");
        vCoord = glGetAttribLocation(mProgramId, "vCoord");
        vMatrix = glGetUniformLocation(mProgramId, "vMatrix");
        vTexture = glGetUniformLocation(mProgramId, "vTexture");
    }

    public void onReady(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int onDrawFrame(int textureId) {
        //设置视窗大小
        glViewport(0, 0, mWidth, mHeight);
        glUseProgram(mProgramId);

        //画画
        //顶点坐标赋值
        mVertexBuffer.position(0);
        //传值
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer);
        //激活
        glEnableVertexAttribArray(vPosition);

        //纹理坐标赋值
        mTextureBuffer.position(0);
        //传值
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer);
        //激活
        glEnableVertexAttribArray(vCoord);

        //片元 vTexture
        //激活图层
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(vTexture, 0);
        onDrawFrameBegin();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindTexture(GL_TEXTURE_2D, 0);
//        glDisableVertexAttribArray(vPosition);
//        glDisableVertexAttribArray(vCoord);
        return textureId;
    }


    public int onDrawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        //设置视窗大小
        glViewport(0, 0, mWidth, mHeight);
        glUseProgram(mProgramId);

        //画画
        //顶点坐标赋值
        vertexBuffer.position(0);
        //传值
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer);
        //激活
        glEnableVertexAttribArray(vPosition);

        //纹理坐标赋值
        textureBuffer.position(0);
        //传值
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer);
        //激活
        glEnableVertexAttribArray(vCoord);

        //片元 vTexture
        //激活图层
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(vTexture, 0);
        onDrawFrameBegin();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindTexture(GL_TEXTURE_2D, 0);
//        glDisableVertexAttribArray(vPosition);
//        glDisableVertexAttribArray(vCoord);
        return textureId;
    }


    public void onDrawFrameBegin() {

    }

    public void release() {
        glDeleteProgram(mProgramId);
    }


    private float view2openglX(float x) {
        return Math.abs((x / mWidth));
    }

    protected float view2openglX(float x, int width) {
        return Math.abs((x / width));
    }

    protected float view2openglY(float y, int height) {
        return Math.abs(y / height);
    }


    ///------------------ 统一变量(uniform)设置 ------------------------///
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    /**
     * 添加延时任务
     *
     * @param runnable
     */
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    /**
     * 运行延时任务
     */
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }
}

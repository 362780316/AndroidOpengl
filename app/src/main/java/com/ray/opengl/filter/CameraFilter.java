package com.ray.opengl.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import com.ray.opengl.R;
import static android.opengl.GLES20.*;

/**
 * 不需要渲染到屏幕（而是写入到 FBO 缓冲中）
 */
public class CameraFilter /* extends BaseFilter*/

        extends BaseFrameFilter {

    // private int[] mFrameBuffers;
    // private int[] mFrameBufferTextures;
    private float[] matrix;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vertex, R.raw.camera_fragment);
    }

    /*@Override
    public void onReady(int width, int height) {
        super.onReady(width, height);
        //创建 FBO （看不见的离屏的屏幕）
        mFrameBuffers = new int[1];
        //        int n, fbo 个数
        //        int[] framebuffers, 用来保存 fbo id 的数组
        //        int offset 从数组   中第几个id来保存
        glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);

        //创建属于 fbo 纹理( 需要配置纹理)
        mFrameBufferTextures = new int[1];
        TextureHelper.genTextures(mFrameBufferTextures);

        glBindTexture(GL_TEXTURE_2D, mFrameBufferTextures[0]);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        //发生关系
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                mFrameBufferTextures[0], 0);
        //解绑
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }*/

    @Override // @param textureId 这里的纹理id 是摄像头的
    public int onDrawFrame(int textureId) {
        glViewport(0, 0, mWidth, mHeight); // 设置视窗大小
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]); // 绑定（否则会绘制到屏幕上了）
        glUseProgram(mProgramId);

        mVertexBuffer.position(0); // 顶点坐标赋值
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer); // 传值
        glEnableVertexAttribArray(vPosition);  // 激活

        // 纹理坐标赋值
        mTextureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer); // 传值
        glEnableVertexAttribArray(vCoord); // 激活

        // TODO 变换矩阵
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);

        // TODO 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        glUniform1i(vTexture, 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知 opengl 绘制
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0); // 解绑 fbo
        // return textureId;
        return mFrameBufferTextures[0]; // TODO 应该返回 FBO 的纹理id
    }

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    @Override
    protected void changeTextureData() {} // 子类可以修改纹理坐标了

    /*private void releaseFrameBuffers() {
        if (null != mFrameBufferTextures) {
            glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (null != mFrameBuffers) {
            glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }
    @Override
    public void release() {
        super.release();
        releaseFrameBuffers();
    }*/
}

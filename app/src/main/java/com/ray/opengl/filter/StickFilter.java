package com.ray.opengl.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import com.ray.opengl.R;
import com.ray.opengl.utils.TextureHelper;

import com.zeusee.main.hyperlandmark.jni.Face;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

// TODO 耳朵 贴纸的专用 过滤类
public class StickFilter extends BaseFrameFilter {
    private Face mFace;
    private final Bitmap mBitmap;
    private int[] mTextureID; // Bitmap转变成纹理ID
    private static final String TAG = "StickFilter";

    public StickFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.base_fragment);
        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.erduo_000);
    }

    @Override
    public void onReady(int width, int height) { // 自定义渲染器来更改更新信息
        super.onReady(width, height);
        mTextureID = new int[1]; // 让Bitmap变成纹理
        TextureHelper.genTextures(mTextureID); // 生成纹理ID

        glBindTexture(GL_TEXTURE_2D, mTextureID[0]);
        ; // 绑定纹理ID 到 纹理2D

        // 这里特殊：不再是像之前像素数据方式，而是Bitmap的专用方式
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, mBitmap, 0); // 级别一般都是0， 边框一般都是0

        glBindTexture(GL_TEXTURE_2D, 0);
        ; // 解除绑定纹理
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
        // return textureID;  // 这里是一个Bug，你要返回大眼后的纹理ID

        // 第二步：红色 耳朵专属图层
        drawStick();

        return mFrameBufferTextures[0];//返回fbo的纹理id
    }

    /**
     * 画耳朵贴纸-在上一层的纹理中 需要混合融合模式，才能贴上去
     */
    private void drawStick() {
        glEnable(GL_BLEND); // 开启混合模式，让贴纸和原纹理混合（融合）

        // sfactor : src原图因子   dfactor : dst目标图因子
        // src:GL_ONE ：全部绘制(耳朵全部保留)
        // dst:GL_ONE_MINUS_SRC_ALPHA ： 1.0 - 源图颜色的alpha作为因子 https://blog.csdn.net/hudfang/article/details/46726465
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA); // 有几种混合模式和因子，可以自行尝试，一般都用这个：GL_ONE_MINUS_SRC_ALPHA

        // 画贴纸耳朵
        float x = mFace.left;
        float y = mFace.top;

        x = x / imgWidth * mWidth;
        y = y / imgHeight * mHeight;

        int newX = (int) x; // 耳朵要根据人脸框框的变换而变换
        int newY = (int) y - mBitmap.getHeight() / 2; // 应该画人脸框框的外面，并且是耳朵贴纸的一半，而不是人脸框框的里面
        int viewWidth = (int) ((float) mFace.width / imgWidth * mWidth); // 人脸框宽 / 送去检测人脸宽 * 改变最新的宽（就是为了相对居中）
        int viewHeight = mBitmap.getHeight(); // 画多高，根据耳朵的高度来
        glViewport(newX, newY, viewWidth, viewHeight);

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
        glBindTexture(GL_TEXTURE_2D, mTextureID[0]); // 绑定
        glUniform1i(vTexture, 0); // 传递参数

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知opengl绘制

        // 下面是解绑FBO
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_BLEND); // 关闭混合模式
    }

    public void setFace(Face mFace) {
        this.mFace = mFace;
    }
}

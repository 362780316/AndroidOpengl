package com.ray.opengl.filter;

import android.content.Context;
import android.util.Log;

import com.ray.opengl.R;

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
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2fv;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

// 对人脸的，一双眼睛 做 局部放大 的专属效果过滤处理
public class ThinFaceFilter extends BaseFrameFilter {

    int u_texSize;//图像分辨率
    int u_preCtrlPoints;//pre控制点
    int u_curCtrlPoints;//cur控制点
    int u_reshapeRadius;//影响半径
    int u_reshapeRatio;//强度
//    int m_MVPMatLoc;

    private FloatBuffer texSizeBuffer; // 图像分辨率的buffer
    private FloatBuffer preCtrlPointBuffer; // pre控制点的buffer
    private FloatBuffer curCtrlPointsBuffer; // cur控制点的buffer

    private Face mFace; // 人脸追踪+人脸5关键点 最终的成果
    public static final String Tag = "ThinFaceFilter";
    int m_FrameIndex;

    public ThinFaceFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.thin_face_fragment);
        u_texSize = glGetUniformLocation(mProgramId, "u_texSize"); // 左眼坐标的属性索引
        u_preCtrlPoints = glGetUniformLocation(mProgramId, "u_preCtrlPoints"); // 右眼坐标的属性索引
        u_curCtrlPoints = glGetUniformLocation(mProgramId, "u_curCtrlPoints"); // 左眼坐标的属性索引
        u_reshapeRadius = glGetUniformLocation(mProgramId, "u_reshapeRadius"); // 右眼坐标的属性索引
        u_reshapeRatio = glGetUniformLocation(mProgramId, "u_reshapeRatio"); // 左眼坐标的属性索引

//        m_MVPMatLoc = glGetUniformLocation(mProgramId, "u_MVPMatrix");

        texSizeBuffer = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();  // 左眼buffer申请空间
        preCtrlPointBuffer = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); // 右眼buffer申请空间
        curCtrlPointsBuffer = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); // 右眼buffer申请空间
    }

    @Override
    public int onDrawFrame(int textureID) {
        if (null == mFace) { // 如果没有找到人脸，就不需要做事情
            return textureID;
        }
        // 1：设置视窗
        glViewport(0, 0, mWidth, mHeight);
        m_FrameIndex++;
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


//        glUniformMatrix4fv(m_MVPMatLoc, 1, GL_FALSE, &m_MVPMatrix[0][0]);
//

        int[] landmarks = mFace.landmarks; // 传 mFace 眼睛坐标 给着色器

        //左脸颊关键点 11
        float LeftCheekKeyPoint[] = {landmarks[20], landmarks[21]};
        Log.v(Tag, "左脸颊关键点:" + LeftCheekKeyPoint[0] + "   " + LeftCheekKeyPoint[1]);
        //下巴关键点   1
        float ChinKeyPoint[] = {landmarks[0], landmarks[1]};
        Log.v(Tag, "下巴关键点:" + ChinKeyPoint[0] + "   " + ChinKeyPoint[1]);
        //右脸颊关键点  16
        float RightCheekPoint[] = {landmarks[30], landmarks[31]};
        Log.v(Tag, "右脸颊关键点:" + RightCheekPoint[0] + "   " + RightCheekPoint[1]);

        float x1 = LeftCheekKeyPoint[0];
        float y1 = LeftCheekKeyPoint[1];
        float x2 = ChinKeyPoint[0];
        float y2 = ChinKeyPoint[1];

        float x3 = RightCheekPoint[0];
        float y3 = RightCheekPoint[1];

        //左侧控制点
//        float LeftSlenderCtlPoint[] = {x1, y2 - 53};
//        float LeftSlenderCtlPoint[] = {x1 - 5, y2 - 50};
        float LeftSlenderCtlPoint[] = {landmarks[164], landmarks[165]};
        Log.v(Tag, "左侧控制点:" + LeftSlenderCtlPoint[0] + "   " + LeftSlenderCtlPoint[1]);
        //右侧控制点

        float RightSlenderCtlPoint[] = {landmarks[198], landmarks[199]};
//        float RightSlenderCtlPoint[] = {x3 + 5, y2 - 50};
//        float RightSlenderCtlPoint[] = {x3 + 6, y2 - 56};
        Log.v(Tag, "右侧控制点:" + RightSlenderCtlPoint[0] + "   " + RightSlenderCtlPoint[1]);

        float effectRadius = (float) ((Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))) / 2);

        float[] PRECTRLPOINT = {LeftSlenderCtlPoint[0] / imgWidth, LeftSlenderCtlPoint[1] / imgHeight,
                RightSlenderCtlPoint[0] / imgWidth, RightSlenderCtlPoint[1] / imgHeight};

        Log.v(Tag, "pre控制坐标" + PRECTRLPOINT[0] * imgWidth + "  " + PRECTRLPOINT[1] * imgHeight + "  " + PRECTRLPOINT[2] * imgWidth + "  " + PRECTRLPOINT[3] * imgHeight);
        preCtrlPointBuffer.clear();
        preCtrlPointBuffer.put(PRECTRLPOINT);
        preCtrlPointBuffer.position(0);

        float leftx = (x1 + x2) / (2 * imgWidth);
        float lefty = (y1 + y2) / (2 * imgHeight);
        float rightx = (x3 + x2) / (2 * imgWidth);
        float righty = (y3 + y2) / (2 * imgHeight);

        float[] CURCTRLPOINT = {leftx, lefty,
                rightx, righty};
        Log.v(Tag, "当前控制坐标" + CURCTRLPOINT[0] * imgWidth + "  " + CURCTRLPOINT[1] * imgHeight + "  " + CURCTRLPOINT[2] * imgWidth + "  " + CURCTRLPOINT[3] * imgHeight);
        curCtrlPointsBuffer.clear();
        curCtrlPointsBuffer.put(CURCTRLPOINT);
        curCtrlPointsBuffer.position(0);

        float[] TEXSIZE = {imgWidth, imgHeight};
        texSizeBuffer.clear();
        texSizeBuffer.put(TEXSIZE);
        texSizeBuffer.position(0);

        float ratio = (m_FrameIndex % 100) * 1.0f / 100;
        ratio = (m_FrameIndex / 100) % 2 == 1 ? (1 - ratio) : ratio;
        Log.v(Tag, "系数ratio:" + ratio);
        glUniform2fv(u_texSize, 1, texSizeBuffer);
        glUniform4fv(u_preCtrlPoints, 1, preCtrlPointBuffer);
        glUniform4fv(u_curCtrlPoints, 1, curCtrlPointsBuffer);
        Log.v(Tag, "影响范围半径:" + effectRadius);
        glUniform1f(u_reshapeRadius, effectRadius);
        glUniform1f(u_reshapeRatio, 0.3f);

        // 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层
        glBindTexture(GL_TEXTURE_2D, textureID); // 绑定
        glUniform1i(vTexture, 0); // 传递参数

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知opengl绘制
        // 解绑fbo
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return mFrameBufferTextures[0];//返回fbo的纹理id

    }


//   private void UpdateMVPMatrix(Matrix4f mvpMatrix, int angleX, int angleY, float ratio)
//    {
////        LOGCATE("FaceSlenderSample::UpdateMVPMatrix angleX = %d, angleY = %d, ratio = %f", angleX, angleY, ratio);
//        angleX = angleX % 360;
//        angleY = angleY % 360;
//
//        //转化为弧度角
//        float radiansX = static_cast<float>(MATH_PI / 180.0f * angleX);
//        float radiansY = static_cast<float>(MATH_PI / 180.0f * angleY);
//
//
//        // Projection matrix
//        Matrix4f Projection =ortho( -1.0f, 1.0f, -1.0f, 1.0f, 0.1f, 100.0f);
//        //glm::mat4 Projection = glm::frustum(-ratio, ratio, -1.0f, 1.0f, 4.0f, 100.0f);
//        //glm::mat4 Projection = glm::perspective(45.0f,ratio, 0.1f,100.f);
//
//        // View matrix
//        glm::mat4 View = glm::lookAt(
//            glm::vec3(0, 0, 4), // Camera is at (0,0,1), in World Space
//        glm::vec3(0, 0, 0), // and looks at the origin
//        glm::vec3(0, 1, 0)  // Head is up (set to 0,-1,0 to look upside-down)
//	);
//
//        // Model matrix
//        glm::mat4 Model = glm::mat4(1.0f);
//        Model = glm::scale(Model, glm::vec3(m_ScaleX, m_ScaleY, 1.0f));
//        Model = glm::rotate(Model, radiansX, glm::vec3(1.0f, 0.0f, 0.0f));
//        Model = glm::rotate(Model, radiansY, glm::vec3(0.0f, 1.0f, 0.0f));
//        Model = glm::translate(Model, glm::vec3(0.0f, 0.0f, 0.0f));
//
//        mvpMatrix = Projection * View * Model;
//
//    }


    public void setFace(Face mFace) { // C++层把人脸最终5关键点成果的(mFaceTrack.getFace()) 赋值给此函数
        this.mFace = mFace;
    }
}

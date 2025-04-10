package com.ray.opengl.record;

import android.content.Context;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import com.ray.opengl.filter.ScreenFilter;

import static android.opengl.EGL14.*;
import static android.opengl.EGL14.eglMakeCurrent;

public class MyEGL {
    private EGLDisplay mEGLDisplay;
    private EGLConfig mEGLConfig;
    private EGLContext mEGLContext;
    private final EGLSurface mEGLSurface;
    private final ScreenFilter mScreenFilter;

    public MyEGL(EGLContext eglContext, Surface surface, Context context, int width, int height) {
        //1, 创建 EGL 环境
        createEGL(eglContext);

        //2，创建窗口（画布）, 绘制线程中的图像，就是往这里创建的 mEGLSurface 上去画
        int[] attrib_list = {
                EGL_NONE//一定要有结尾符
        };
        mEGLSurface = eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface,
                attrib_list, 0);

        //3，让 画布 盖住屏幕( 让 mEGLDisplay 和 mEGLSurface 发生绑定关系)
        if(!eglMakeCurrent(mEGLDisplay,
                mEGLSurface,
                mEGLSurface,
                mEGLContext
                )){
            throw new RuntimeException("eglMakeCurrent fail");
        }
        //4，往虚拟屏幕上画画
        mScreenFilter = new ScreenFilter(context);
        mScreenFilter.onReady(width, height);
    }

    private void createEGL(EGLContext share_eglContext) {
        //1, 获取显示设备，EGL_DEFAULT_DISPLAY： 默认设备（手机屏幕）
        mEGLDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);

        //2, 初始化设备
        int[] version = new int[2];
        if(!eglInitialize(mEGLDisplay, version, 0, version, 1)){
            throw new RuntimeException("eglInitialize fail");
        }
        //3， 选择配置
        int[] attrib_list = {
                //像素格式 rgba
                EGL_RED_SIZE,  8,
                EGL_GREEN_SIZE,  8,
                EGL_BLUE_SIZE,  8,
                EGL_ALPHA_SIZE,  8,
                //指定渲染api类型
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                //告诉egl 以android兼容方式创建 surface
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL_NONE//一定要有结尾符
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        if(!eglChooseConfig(
                mEGLDisplay,
                attrib_list,//属性列表
                0 ,
                configs,
                0,
                configs.length,
                num_config,
                0
            )){
            throw new RuntimeException("eglChooseConfig fail");
        }
        mEGLConfig = configs[0];

        // 4，创建上下文
//        EGLDisplay dpy,
//        EGLConfig config,
//        EGLContext share_context,
//        int[] attrib_list,
//        int offset
        int[] ctx_attrib_list = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_NONE//一定要有结尾符
        };
        mEGLContext = eglCreateContext(
                mEGLDisplay,
                mEGLConfig,
                share_eglContext,//共享上下文， 绘制线程 GLThread 中 EGL上下文，达到资源共享
                ctx_attrib_list,
                0);
        if(null == mEGLContext || mEGLContext == EGL_NO_CONTEXT){
            mEGLContext = null;
            throw new RuntimeException("eglCreateContext fail");
        }

    }

    /**
     * 画画
     */
    public void draw(int textureId, long timestamp){
        //在虚拟屏幕上渲染
        mScreenFilter.onDrawFrame(textureId);
        //刷新时间戳(如果设置不合理，编码时会采取丢帧或降低视频质量方式进行编码)
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, timestamp);
        //交换缓冲区数据
        eglSwapBuffers(mEGLDisplay, mEGLSurface);
    }

    /**
     * 释放资源
     */
    public void release(){
        eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
        eglDestroySurface(mEGLDisplay, mEGLSurface);
        eglDestroyContext(mEGLDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEGLDisplay);
    }
}

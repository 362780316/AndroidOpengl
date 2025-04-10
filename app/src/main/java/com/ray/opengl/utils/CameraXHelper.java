package com.ray.opengl.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.ray.opengl.MyPreviewCallback;
import com.google.common.util.concurrent.ListenableFuture;

import static android.graphics.ImageFormat.NV21;
import static androidx.camera.core.CameraX.LensFacing.FRONT;

public class CameraXHelper implements ImageAnalysis.Analyzer, SurfaceHolder.Callback {

    private static final String TAG = "CameraHelper";

    public int getCameraID() {
        return mCameraID;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    private int mCameraID;
    private int mWidth;
    private int mHeight;
    private byte[] cameraBuffer;
    private byte[] cameraBuffer_;
    private SurfaceHolder mSurfaceHolder;
    private CameraX camera;
    private OnChangedSizeListener mOnChangedSizeListener;
    private int mRotation;
    private SurfaceTexture mSurfaceTexture;
    private MyPreviewCallback mPreviewCallback;
    LifecycleOwner lifecycleOwner;
    Preview.OnPreviewOutputUpdateListener onPreviewOutputUpdateListener;
    Preview preview;
    Context context;


    public CameraXHelper(LifecycleOwner lifecycleOwner, Preview.OnPreviewOutputUpdateListener onPreviewOutputUpdateListener, int cameraId, int width, int height) {
        this.lifecycleOwner = lifecycleOwner;
        this.onPreviewOutputUpdateListener = onPreviewOutputUpdateListener;
        mCameraID = cameraId;
        mWidth = width;
        mHeight = height;
        getPreView();
    }


    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 释放摄像头
//        stopPreview();
        // 开启摄像头
        startPreview(mSurfaceTexture);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }


    /**
     * 开始预览
     */
    public void startPreview(final SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        try {
            cameraBuffer = new byte[mWidth * mHeight * 3 / 2];
            cameraBuffer_ = new byte[mWidth * mHeight * 3 / 2];
//            getPreView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void rotation90(byte[] data) {
        int index = 0;
        int ySize = mWidth * mHeight;
        // u和v
        int uvHeight = mHeight / 2;
        // 后置摄像头顺时针旋转90度
        if (mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
            // 将y的数据旋转之后 放入新的byte数组
            for (int i = 0; i < mWidth; i++) {
                for (int j = mHeight - 1; j >= 0; j--) {
                    cameraBuffer_[index++] = data[mWidth * j + i];
                }
            }

            // 每次处理两个数据
            for (int i = 0; i < mWidth; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    cameraBuffer_[index++] = data[ySize + mWidth * j + i];
                    // u
                    cameraBuffer_[index++] = data[ySize + mWidth * j + i + 1];
                }
            }
        } else {
            // 逆时针旋转90度
            //            for (int i = 0; i < mWidth; i++) {
            //                for (int j = 0; j < mHeight; j++) {
            //                    cameraBuffer_[index++] = data[mWidth * j + mWidth - 1 - i];
            //                }
            //            }
            //            //  u v
            //            for (int i = 0; i < mWidth; i += 2) {
            //                for (int j = 0; j < uvHeight; j++) {
            //                    cameraBuffer_[index++] = data[ySize + mWidth * j + mWidth - 1 - i - 1];
            //                    cameraBuffer_[index++] = data[ySize + mWidth * j + mWidth - 1 - i];
            //                }
            //            }

            // 旋转并镜像
            for (int i = 0; i < mWidth; i++) {
                for (int j = mHeight - 1; j >= 0; j--) {
                    cameraBuffer_[index++] = data[mWidth * j + mWidth - 1 - i];
                }
            }
            //  u v
            for (int i = 0; i < mWidth; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    cameraBuffer_[index++] = data[ySize + mWidth * j + mWidth - 1 - i - 1];
                    // u
                    cameraBuffer_[index++] = data[ySize + mWidth * j + mWidth - 1 - i];
                }
            }
        }
    }

    public void setPreviewCallback(MyPreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        // 旋转数据，已经被删除了
       /*switch (mRotation) {
        case Surface.ROTATION_0:
            rotation90(data);
                break;
        case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                break;
        case Surface.ROTATION_270:// 横屏 头部在右边
                break;
        }*/
        byte[] data = Utils.getDataFromImage(image);
        int format = image.getFormat();
        Log.v("ray-format", image.getWidth() + "---" + image.getHeight());

//        byte[] data = Utils.yuv420ToNv21(image);
        // TODO 注意：你把上面的代码给注释掉了，意味着：data数据依然是颠倒的，再把颠倒的数据交给OpenCV内置函数旋转更简单
        if (mPreviewCallback != null) {
            mPreviewCallback.onPreviewFrame(data, camera, rotationDegrees);
            //  mPreviewCallback.onPreviewFrame(cameraBuffer_, camera);
        }
//        camera.addCallbackBuffer(cameraBuffer);
    }

    public void stopPreview() {
    }

    public interface OnChangedSizeListener {
        void onChanged(int width, int height);
    }

    private void getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(FRONT) //前置或者后置摄像头
                .build();
        preview = new Preview(previewConfig);


//        HandlerThread handlerThread = new HandlerThread("Analyze-thread");
//        handlerThread.start();

        ImageAnalysisConfig imageAnalysisConfig =
                new ImageAnalysisConfig.Builder()
                        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
//                        .setCallbackHandler(new Handler(handlerThread.getLooper()))
                        .setTargetResolution(new Size(mWidth, mHeight))
                        .setLensFacing(FRONT)
//                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);

        //图片拍摄的配置config
        ImageCaptureConfig.Builder captureBuilder = new ImageCaptureConfig.Builder().setLensFacing(FRONT);
        ImageCapture imageCapture = new ImageCapture(captureBuilder.build());

        CameraX.unbindAll();
        //预览数据添加回调，老版本1.0.0alpha05版本有，而且思路很清晰，
        // 我在用的1.0.0rc03已经没有了，虽然也能获取但是就贼费事了
        preview.setOnPreviewOutputUpdateListener(onPreviewOutputUpdateListener);
        CameraX.bindToLifecycle(lifecycleOwner, preview, imageAnalysis, imageCapture);
    }

    public int getmWidth() {
        return mWidth;
    }

    public int getmHeight() {
        return mHeight;
    }
}


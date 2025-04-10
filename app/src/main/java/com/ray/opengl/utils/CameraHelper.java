package com.ray.opengl.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.Iterator;
import java.util.List;

public class CameraHelper implements Camera.PreviewCallback, SurfaceHolder.Callback {

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

    private Activity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;

    private Camera.PreviewCallback mPreviewCallback;
    private OnChangedSizeListener mOnChangedSizeListener;
    private int mRotation;
    private SurfaceTexture mSurfaceTexture;
    private int CameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    public CameraHelper(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraID = cameraId;
        mWidth = width;
        mHeight = height;
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview(mSurfaceTexture);
    }

    /**
     * 开始预览
     */
    public void startPreview(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        try {
            // 获得camera对象
            mCamera = Camera.open(mCameraID);
            // 配置camera的属性
            Camera.Parameters parameters = mCamera.getParameters();
            // 设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            // 这是摄像头宽、高
            setPreviewSize(parameters);
            // 设置摄像头 图像传感器的角度、方向
            setPreviewOrientation(parameters);
            mCamera.setParameters(parameters);
            cameraBuffer = new byte[mWidth * mHeight * 3 / 2];
            cameraBuffer_ = new byte[mWidth * mHeight * 3 / 2];
            // 数据缓存区
            mCamera.addCallbackBuffer(cameraBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);

            // mCamera.setPreviewDisplay(mSurfaceHolder); // 设置预览画面（以前的方式）

            mCamera.setPreviewTexture(surfaceTexture);  // 离屏渲染（现在的方式）
//            initCamera();
            /*if (mOnChangedSizeListener != null) {
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
            }*/

            mCamera.startPreview(); // 开启预览
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCamera() {
        if (null != mCamera) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> flashModes = parameters.getSupportedFlashModes();
                if(flashModes !=null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
                {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }


                List<Camera.Size> pictureSizes = mCamera.getParameters()
                        .getSupportedPictureSizes();

                parameters.setPreviewSize(mWidth, mHeight);

                Camera.Size fs = null;
                for (int i = 0; i < pictureSizes.size(); i++) {
                    Camera.Size psize = pictureSizes.get(i);
                    if (fs == null && psize.width >= 1280)
                        fs = psize;

                }
                parameters.setPictureSize(fs.width, fs.height);

                if (mActivity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    parameters.set("rotation", 90);
                    int orientation = CameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 360 - mCameraInfo.orientation : mCameraInfo.orientation;
                    mCamera.setDisplayOrientation(orientation);

                } else {
                    parameters.set("orientation", "landscape");
                    mCamera.setDisplayOrientation(0);

                }

                if(CameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK){
                    if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    } else{
                        parameters.setFocusMode(parameters.FOCUS_MODE_AUTO);
                    }
                }

                mCamera.setParameters(parameters);
                mCamera.setPreviewCallback(this.mPreviewCallback);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            // 预览数据回调接口
            mCamera.setPreviewCallback(null);
            // 停止预览
            mCamera.stopPreview();
            // 释放摄像头
            mCamera.release();
            mCamera = null;
        }
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        // 获取摄像头支持的宽、高
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        Log.d(TAG, "Camera支持: " + size.width + "x" + size.height);
        // 选择一个与设置的差距最小的支持分辨率
        int m = Math.abs(size.width * size.height - mWidth * mHeight);
        supportedPreviewSizes.remove(0);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        // 遍历
        while (iterator.hasNext()) {
            Camera.Size next = iterator.next();
            Log.d(TAG, "支持 " + next.width + "x" + next.height);
            int n = Math.abs(next.height * next.width - mWidth * mHeight);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        mWidth = size.width;
        mHeight = size.height;
        parameters.setPreviewSize(mWidth, mHeight);
        Log.d(TAG, "预览分辨率 width:" + mWidth + " height:" + mHeight);
    }

    public int getmRotation() {
        return mRotation;
    }

    protected Camera.CameraInfo mCameraInfo = null;

    private void setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraInfo = info;
        }
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mHeight, mWidth);
                }
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                degrees = 90;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mWidth, mHeight);
                }
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mHeight, mWidth);
                }
                break;
            case Surface.ROTATION_270:// 横屏 头部在右边
                degrees = 270;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mWidth, mHeight);
                }
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        // 设置角度, 参考源码注释
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 释放摄像头
        stopPreview();
        // 开启摄像头
        startPreview(mSurfaceTexture);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.v("onPreviewFrame",mRotation+"");
        // 旋转数据，已经被删除了


//       switch (mRotation) {
//        case Surface.ROTATION_0:
//            rotation90(data);
////            data = rotateYUV420Degree180(data,mWidth,mHeight);
//                break;
//        case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
//                break;
//        case Surface.ROTATION_270:// 横屏 头部在右边
//                break;
//        }

        // TODO 注意：你把上面的代码给注释掉了，意味着：data数据依然是颠倒的，再把颠倒的数据交给OpenCV内置函数旋转更简单
        if (mPreviewCallback != null) {
            mPreviewCallback.onPreviewFrame(data, camera);
//              mPreviewCallback.onPreviewFrame(cameraBuffer_, camera);
        }
        camera.addCallbackBuffer(cameraBuffer);
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

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    public interface OnChangedSizeListener {
        void onChanged(int width, int height);
    }

    public int getmWidth() {
        return mWidth;
    }

    public int getmHeight() {
        return mHeight;
    }


    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
                        + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    private static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth,
                                               int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;// uvHeight = height / 2
        }

        int k = 0;
        for (int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }
        for (int i = 0; i < imageWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        return rotateYUV420Degree180(rotateYUV420Degree90(data, imageWidth, imageHeight), imageWidth, imageHeight);
    }


    public int getOrientation(){
        if(mCameraInfo != null){
            return mCameraInfo.orientation;
        }
        return 0;
    }
}


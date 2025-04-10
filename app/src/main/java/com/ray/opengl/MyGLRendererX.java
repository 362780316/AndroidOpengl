//package com.ray.opengl;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Matrix;
//import android.graphics.Paint;
//import android.graphics.PointF;
//import android.graphics.PorterDuff;
//import android.graphics.Rect;
//import android.graphics.SurfaceTexture;
//import android.hardware.Camera;
//import android.opengl.EGL14;
//import android.opengl.EGLContext;
//import android.opengl.GLSurfaceView;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.util.Log;
//import android.view.SurfaceView;
//
//import androidx.camera.core.CameraX;
//import androidx.camera.core.Preview;
//import androidx.lifecycle.LifecycleOwner;
//
//import com.ray.opengl.face.FaceTrack;
//import com.ray.opengl.filter.BeautyFilter;
//import com.ray.opengl.filter.BigEyeFilter22222;
//import com.ray.opengl.filter.CameraFilter;
//import com.ray.opengl.filter.ScreenFilter;
//import com.ray.opengl.filter.StickFilter;
//import com.ray.opengl.record.MyMediaRecorder;
//import com.ray.opengl.utils.CameraXHelper;
//import com.ray.opengl.utils.FileUtil;
//import com.ray.opengl.utils.STUtils;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.List;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
//import com.zeusee.main.hyperlandmark.jni.Face;
//import com.zeusee.main.hyperlandmark.jni.FaceTracking;
//
//import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
//import static android.opengl.GLES20.glClear;
//import static android.opengl.GLES20.glClearColor;
//
//// import com.ray.opengl.filter.BeautyFilter;
//
//public class MyGLRendererX implements GLSurfaceView.Renderer,
//        SurfaceTexture.OnFrameAvailableListener, MyPreviewCallback, Preview.OnPreviewOutputUpdateListener {
//
//    private MyGLSurfaceView mGLSurfaceView;
//    private CameraXHelper mCameraHelper;
//    private int[] mTextureID;
//    private SurfaceTexture mSurfaceTexture;
//    private ScreenFilter mScreenFilter;
//    private CameraFilter mCameraFilter;
//    private float[] mtx = new float[16];
//    private MyMediaRecorder mMediaRecorder;
//    private int mWidth;
//    private int mHeight;
//
//    private BigEyeFilter22222 mBigEyeFilter; // TODO 【大眼相关代码】
//    private FaceTrack mFaceTrack; // TODO 【大眼相关代码】
//    private StickFilter mStickFilter; // TODO 【贴纸相关代码】
//    private BeautyFilter mBeautyFilter; // TODO 【美颜相关代码】
////    private DemoFilter mDemoFilter;
//
//    private FaceTracking mFaceTracking;
//    private boolean mTrack106 = false;
//    byte[] mNv21Data;
//    private byte[] mTmpBuffer;
//
//    private HandlerThread mHandlerThread;
//    private final Object lockObj = new Object();
//    private Handler mHandler;
//
//    int CameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
//    protected SurfaceView mOverlap = null;
//    Paint mPaint;
//    final int PREVIEW_WIDTH = 640;
//    final int PREVIEW_HEIGHT = 480;
//    Matrix matrix = new Matrix();
//
//
//    //    GLPoints mPoints;
////EGLUtils mEglUtils;
//    Context context;
//
//    public MyGLRendererX(Context context, MyGLSurfaceView myGLSurfaceView) {
//        this.context = context;
//        mGLSurfaceView = myGLSurfaceView;
//
//        mCameraHelper = new CameraXHelper((LifecycleOwner) context, this,
//                Camera.CameraInfo.CAMERA_FACING_FRONT, PREVIEW_WIDTH, PREVIEW_HEIGHT);
//        mCameraHelper.setPreviewCallback(this);
//
//        // TODO 【大眼相关代码】  assets Copy到SD卡
//        FileUtil.copyAssets2SDCard(mGLSurfaceView.getContext(), "lbpcascade_frontalface.xml",
//                "/sdcard/lbpcascade_frontalface.xml"); // OpenCV的模型
//        FileUtil.copyAssets2SDCard(mGLSurfaceView.getContext(), "seeta_fa_v1.1.bin",
//                "/sdcard/seeta_fa_v1.1.bin"); // 中科院的模型
//
//        InitModelFiles();
//        mFaceTracking = new FaceTracking("/sdcard/ZeuseesFaceTracking/models");
//
//        mPaint = new Paint();
//        mPaint.setColor(Color.rgb(57, 138, 243));
//        int strokeWidth = Math.max(PREVIEW_HEIGHT / 240, 2);
//        mPaint.setStrokeWidth(strokeWidth);
//        mPaint.setStyle(Paint.Style.FILL);
//    }
//
//
//    /**
//     * Surface 创建时 回调
//     *
//     * @param gl     1.0 api遗留参数
//     * @param config
//     */
//    @Override
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        // 准备摄像头绘制的画布
//        mTextureID = new int[1];
////        glGenTextures(mTextureID.length, mTextureID, 0);
////        mSurfaceTexture = new SurfaceTexture(mTextureID[0]);
//        mSurfaceTexture.attachToGLContext(mTextureID[0]);
//        mSurfaceTexture.setOnFrameAvailableListener(this);
//
//        mCameraFilter = new CameraFilter(mGLSurfaceView.getContext());
//        mScreenFilter = new ScreenFilter(mGLSurfaceView.getContext());
//
//        EGLContext eglContext = EGL14.eglGetCurrentContext();
//
//        mMediaRecorder = new MyMediaRecorder(480, 640,
//                "/sdcard/test_" + System.currentTimeMillis() + ".mp4", eglContext,
//                mGLSurfaceView.getContext());
//
//
//        mNv21Data = new byte[mCameraHelper.getmWidth() * mCameraHelper.getmHeight() * 2];
//        mTmpBuffer = new byte[mCameraHelper.getmWidth() * mCameraHelper.getmHeight() * 2];
//
//        mHandlerThread = new HandlerThread("DrawFacePointsThread");
//        mHandlerThread.start();
//        mHandler = new Handler(mHandlerThread.getLooper());
//    }
//
//
//    /**
//     * Surface 发生改变回调
//     *
//     * @param gl
//     * @param width  720   OpenGL相关的
//     * @param height 1022  OpenGL相关的
//     */
//    @Override
//    public void onSurfaceChanged(GL10 gl, int width, int height) {
//        mWidth = width;
//        mHeight = height;
//
//        // 创建人脸检测跟踪器 // TODO 【大眼相关的人脸追踪/人脸关键点的代码】
//        mFaceTrack = new FaceTrack("/sdcard/lbpcascade_frontalface.xml", "/sdcard/seeta_fa_v1.1.bin", mCameraHelper);
//        mFaceTrack.startTrack(); // 启动跟踪器
//
//        mCameraHelper.startPreview(mSurfaceTexture);
//
//        mCameraFilter.onReady(width, height);
//        mScreenFilter.onReady(width, height);
//
//        matrix.setScale(width / (float) PREVIEW_HEIGHT, height / (float) PREVIEW_WIDTH);
////        mEglUtils = new EGLUtils();
////        mEglUtils.initEGL(mGLSurfaceView.getHolder().getSurface());
//    }
//
//    /**
//     * 绘制一帧图像时 回调
//     * 注意：该方法中一定要进行绘制操作
//     * 该方法返回后，会交换渲染缓冲区，如果不绘制任何东西，会导致屏幕闪烁
//     *
//     * @param gl
//     */
//    @Override
//    public void onDrawFrame(GL10 gl) {
//        glClearColor(255, 0, 0, 0);//屏幕清理颜色 红色
//        // mask
//        // GL_COLOR_BUFFER_BIT 颜色缓冲区
//        // GL_DEPTH_BUFFER_BIT 深度
//        // GL_STENCIL_BUFFER_BIT 模型
//        glClear(GL_COLOR_BUFFER_BIT);
//
//        // 绘制摄像头数据
//        mSurfaceTexture.updateTexImage();
//        mSurfaceTexture.getTransformMatrix(mtx);
//
//        mCameraFilter.setMatrix(mtx);
//        // mCameraFilter： 摄像头数据先输出到 fbo
//        int textureId = mCameraFilter.onDrawFrame(mTextureID[0]);
//
//        // TODO 【大眼相关代码】
//        // TODO textureId = 大眼Filter.onDrawFrame(textureId);
//        if (null != mBigEyeFilter) {
//            mBigEyeFilter.setFace(mFaceTrack.getFace());
//            textureId = mBigEyeFilter.onDrawFrame(textureId);
//        }
//
//        // TODO 【贴纸相关代码】
//        if (null != mStickFilter) {
//            mStickFilter.setFace(mFaceTrack.getFace()); // 需要定位人脸，所以需要 JavaBean
//            textureId = mStickFilter.onDrawFrame(textureId);
//        }
//
//        // TODO 【美颜相关代码】
//        if (null != mBeautyFilter) { // 没有不需要 人脸追踪/人脸关键点，整个屏幕美颜
//            textureId = mBeautyFilter.onDrawFrame(textureId);
//        }
//
//        // TODO 【关键点】
////        if (null != mDemoFilter) { // 没有不需要 人脸追踪/人脸关键点，整个屏幕美颜
////            mDemoFilter.setFace(mFaceTrack.getFace());
////            textureId = mDemoFilter.onDrawFrame(textureId);
////        }
//
//        // textureId = xxxFilter.onDrawFrame(textureId);
//        // ... textureId == 大眼后的纹理ID
//        mScreenFilter.onDrawFrame(textureId);
//
//        // 录制
//        mMediaRecorder.encodeFrame(textureId, mSurfaceTexture.getTimestamp());
//    }
//
//    /**
//     * surfaceTexture 画布有一个有效的新图像时 回调
//     *
//     * @param surfaceTexture
//     */
//    @Override
//    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//        mGLSurfaceView.requestRender();
//    }
//
//    public void surfaceDestroyed() {
//        mCameraHelper.stopPreview();
//        mFaceTrack.stopTrack(); // 停止跟踪器
//    }
//
//    /**
//     * 开始录制
//     *
//     * @param speed
//     */
//    public void startRecording(float speed) {
//        Log.e("MyGLRender", "startRecording");
//        try {
//            mMediaRecorder.start(speed);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 停止录制
//     */
//    public void stopRecording() {
//        Log.e("MyGLRender", "stopRecording");
//        mMediaRecorder.stop();
//    }
//
//    /**
//     * // TODO 【大眼相关代码】
//     * 开启大眼特效
//     *
//     * @param isChecked
//     */
//    public void enableBigEye(final boolean isChecked) {
//        // BigEyeFilter bigEyeFilter = new BigEyeFilter(); // 这样可以吗  不行，必须在EGL线程里面绘制
//
//        mGLSurfaceView.queueEvent(new Runnable() { // 把大眼渲染代码，加入到， GLSurfaceView 的 内置EGL 的 GLTHread里面
//            public void run() {
//                if (isChecked) {
//                    mBigEyeFilter = new BigEyeFilter22222(mGLSurfaceView.getContext());
//                    mBigEyeFilter.onReady(mWidth, mHeight);
//                } else {
//                    mBigEyeFilter.release();
//                    mBigEyeFilter = null;
//                }
//            }
//        });
//    }
//
////    // Camera画面只有有数据，就会回调此函数
////    @Override // 要把相机的数据，给C++层做人脸追踪  // TODO 【大眼相关代码】
////    public void onPreviewFrame(final byte[] data, Camera camera) {
////
////    }
//
//    private float view2openglX(float x, int width) {
//        return Math.abs((x / width));
//    }
//
//    private float view2openglY(float y, int height) {
//        return Math.abs(y / height);
//    }
//
//
//    // TODO 下面是 NDK OpenGL 53节课新增点
//
//    /**
//     * TODO 开启贴纸
//     *
//     * @param isChecked checkbox复选框是否勾上了
//     */
//    public void enableStick(final boolean isChecked) {
//        mGLSurfaceView.queueEvent(new Runnable() { // 在EGL线程里面绘制 贴纸工作
//            public void run() {
//                if (isChecked) {
//                    mStickFilter = new StickFilter(mGLSurfaceView.getContext());
//                    mStickFilter.onReady(mWidth, mHeight);
//                } else {
//                    mStickFilter.release();
//                    mStickFilter = null;
//                }
//            }
//        });
//    }
//
//
//    public void enableDemo(final boolean isChecked) {
////        mGLSurfaceView.queueEvent(new Runnable() { // 在EGL线程里面绘制 贴纸工作
////            public void run() {
////                if (isChecked) {
////                    mDemoFilter = new DemoFilter(mGLSurfaceView.getContext());
////                    mDemoFilter.onReady(mWidth, mHeight);
////                } else {
//////                    mDemoFilter.release();
////                    mDemoFilter = null;
////                }
////            }
////        });
//    }
//
//    /**
//     * TODO 开启美颜
//     *
//     * @param isChecked checkbox复选框是否勾上了
//     */
//    public void enableBeauty(final boolean isChecked) {
//        mGLSurfaceView.queueEvent(new Runnable() {
//            public void run() {
//                if (isChecked) {
//                    mBeautyFilter = new BeautyFilter(mGLSurfaceView.getContext());
//                    mBeautyFilter.onReady(mWidth, mHeight);
//                } else {
//                    mBeautyFilter.release();
//                    mBeautyFilter = null;
//                }
//            }
//        });
//    }
//
//    void InitModelFiles() {
//        String assetPath = "ZeuseesFaceTracking";
//        String sdcardPath = Environment.getExternalStorageDirectory()
//                + File.separator + assetPath;
//        copyFilesFromAssets(mGLSurfaceView.getContext(), assetPath, sdcardPath);
//    }
//
//
//    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
//        try {
//            String[] fileNames = context.getAssets().list(oldPath);
//            if (fileNames.length > 0) {
//                // directory
//                File file = new File(newPath);
//                if (!file.mkdir()) {
//                    Log.d("mkdir", "can't make folder");
//                }
//
//                for (String fileName : fileNames) {
//                    copyFilesFromAssets(context, oldPath + "/" + fileName,
//                            newPath + "/" + fileName);
//                }
//            } else {
//                // file
//                InputStream is = context.getAssets().open(oldPath);
//                FileOutputStream fos = new FileOutputStream(new File(newPath));
//                byte[] buffer = new byte[1024];
//                int byteCount;
//                while ((byteCount = is.read(buffer)) != -1) {
//                    fos.write(buffer, 0, byteCount);
//                }
//                fos.flush();
//                is.close();
//                fos.close();
//            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    public Matrix getMatrix() {
//        return matrix;
//    }
//
//    @Override
//    public void onUpdated(Preview.PreviewOutput output) {
//        mSurfaceTexture = output.getSurfaceTexture();
//    }
//
//    @Override
//    public void onPreviewFrame(byte[] data, CameraX camera, int rotationDegrees) {
//        synchronized (lockObj) {
//            System.arraycopy(data, 0, mNv21Data, 0, data.length);
//        }
////        mHandler02.removeMessages(MESSAGE_DRAW_POINTS);
////        mHandler02.sendEmptyMessage(MESSAGE_DRAW_POINTS);
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
////                mFaceTrack.detector(mNv21Data);
//                if (mTrack106) {
//                    mFaceTracking.FaceTrackingInit(mNv21Data, mCameraHelper.getmHeight(), mCameraHelper.getmWidth());
//                    mTrack106 = !mTrack106;
//                } else {
//                    mFaceTracking.Update(mNv21Data, mCameraHelper.getmHeight(), mCameraHelper.getmWidth());
//                }
//
////                Log.v("camera-Orientation", mCameraHelper.getOrientation() + "");
////                boolean rotate270 = mCameraHelper.getOrientation() == 270;
//                boolean rotate270 = true;
//                List<Face> faceActions = mFaceTracking.getTrackingInfo();
//                Log.e("faceActions", faceActions.toString());
//
//                if (mOverlap == null) {
//                    mOverlap = ((MainActivity) context).getmOverlap();
//                }
//                if (!mOverlap.getHolder().getSurface().isValid()) {
//                    return;
//                }
//                Canvas canvas = mOverlap.getHolder().lockCanvas();
//                if (canvas == null)
//                    return;
//                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
//                canvas.setMatrix(getMatrix());
//
//                if (faceActions.size() == 1) {
//                    Face face = faceActions.get(0);
//                    Log.e("v2人脸检测", face.toString());
//                    float[] points = null;
//                    for (Face r : faceActions) {
//                        points = new float[106 * 2];
////                        Rect rect = new Rect(mCameraHelper.getmHeight() - r.left, r.top, mCameraHelper.getmHeight() - r.right, r.bottom);
//                        for (int i = 0; i < 106; i++) {
//                            int x;
//                            if (rotate270) {
//                                x = r.landmarks[i * 2];
//                            } else {
//                                x = mCameraHelper.getmHeight() - r.landmarks[i * 2];
//                            }
//                            int y = r.landmarks[i * 2 + 1];
//                            points[i * 2] = view2openglX(x, mCameraHelper.getmHeight());
//                            points[i * 2 + 1] = view2openglY(y, mCameraHelper.getmWidth());
//                        }
//                        if (face != null) {
//                            com.ray.opengl.face.Face faceRet = new com.ray.opengl.face.Face(face.width, face.height,
//                                    mCameraHelper.getmWidth(), mCameraHelper.getmHeight(), points
//                            );
//                            mFaceTrack.setmFace(faceRet);
//                        }
//
//                        Rect rect = new Rect(mCameraHelper.getmHeight() - r.left, r.top, mCameraHelper.getmHeight() - r.right, r.bottom);
//                        PointF[] points02 = new PointF[106];
//                        for (int i = 0; i < 106; i++) {
//                            points02[i] = new PointF(r.landmarks[i * 2], r.landmarks[i * 2 + 1]);
//                        }
//                        float[] visibles = new float[106];
//
//                        for (int i = 0; i < points02.length; i++) {
//                            visibles[i] = 1.0f;
//                            if (rotate270) {
//                                points02[i].x = mCameraHelper.getmHeight() - points02[i].x;
//                            }
//                        }
//
//                        STUtils.drawFaceRect(canvas, rect, mCameraHelper.getmHeight(),
//                                mCameraHelper.getmWidth(), true);
//                        STUtils.drawPoints(canvas, mPaint, points02, visibles, mCameraHelper.getmHeight(),
//                                mCameraHelper.getmWidth(), true);
//                    }
//                }
//
////                for (Face r : faceActions) {
////
////                    Rect rect = new Rect(PREVIEW_HEIGHT - r.left, r.top, PREVIEW_HEIGHT - r.right, r.bottom);
////
////                    PointF[] points = new PointF[106];
////                    for (int i = 0; i < 106; i++) {
////                        points[i] = new PointF(r.landmarks[i * 2], r.landmarks[i * 2 + 1]);
////                    }
////
////                    float[] visibles = new float[106];
////
////
////                    for (int i = 0; i < points.length; i++) {
////                        visibles[i] = 1.0f;
////                        if (rotate270) {
////                            points[i].x = PREVIEW_HEIGHT - points[i].x;
////                        }
////                    }
////
////                    STUtils.drawFaceRect(canvas, rect, PREVIEW_HEIGHT,
////                            PREVIEW_WIDTH, true);
////                    STUtils.drawPoints(canvas, mPaint, points, visibles, PREVIEW_HEIGHT,
////                            PREVIEW_WIDTH, true);
////
////                }
//                mOverlap.getHolder().unlockCanvasAndPost(canvas);
//            }
//
//        });
//    }
//}

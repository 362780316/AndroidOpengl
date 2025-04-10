package com.ray.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

// import com.ray.opengl.filter.BeautyFilter;
import com.google.android.filament.TransformManager;
import com.google.android.filament.Viewport;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.utils.Float3;
import com.google.android.filament.utils.MatrixKt;
import com.google.android.filament.utils.ModelViewer;
import com.ray.opengl.filter.BeautyFilter;
import com.ray.opengl.filter.BigEyeFilter22222;
import com.ray.opengl.filter.BrightEyeFilter;
import com.ray.opengl.filter.CameraFilter;
import com.ray.opengl.filter.CatFilter;
import com.ray.opengl.filter.LipstickFilter;
import com.ray.opengl.filter.ScreenFilter;
import com.ray.opengl.filter.StickFilter;
import com.ray.opengl.filter.ThinFaceFilter;
import com.ray.opengl.filter.beauty.GLImageBeautyFilter;
import com.ray.opengl.makeup.bean.DynamicMakeup;
import com.ray.opengl.record.MyMediaRecorder;
import com.ray.opengl.resource.MakeupHelper;
import com.ray.opengl.resource.ResourceHelper;
import com.ray.opengl.resource.ResourceJsonCodec;
import com.ray.opengl.resource.bean.ResourceData;
import com.ray.opengl.stickers.bean.DynamicSticker;
import com.ray.opengl.utils.CameraHelper;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.ray.opengl.utils.ToastUtil;
import com.zeusee.main.hyperlandmark.jni.Face;
import com.zeusee.main.hyperlandmark.jni.FaceTracking;

import static android.opengl.GLES20.*;
import static com.google.android.filament.utils.VectorKt.max;

public class MyGLRenderer implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {

    private MyGLSurfaceView mGLSurfaceView;
    private CameraHelper mCameraHelper;
    private int[] mTextureID;
    private SurfaceTexture mSurfaceTexture;
    private ScreenFilter mScreenFilter;
    private CameraFilter mCameraFilter;
    private float[] mtx = new float[16];
    private MyMediaRecorder mMediaRecorder;
    private int mWidth;
    private int mHeight;

    private BigEyeFilter22222 mBigEyeFilter;
    private StickFilter mStickFilter;
    private BeautyFilter mBeautyFilter;
    //    private DemoFilter mDemoFilter;
    private ThinFaceFilter thinFaceFilter;
    private BrightEyeFilter brightEyeFilter;
    private LipstickFilter lipstickFilter;
    private GLImageBeautyFilter glImageBeautyFilter;
    private CatFilter catFilter;


    private FaceTracking mFaceTracking;
    private boolean mTrack106 = false;
    byte[] mNv21Data;
    private byte[] mTmpBuffer;

    private HandlerThread mHandlerThread;
    private final Object lockObj = new Object();
    private Handler mHandler;
    int CameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    protected SurfaceView mOverlap = null;
    Paint mPaint;
    private final int PREVIEW_WIDTH = 640;
    private final int PREVIEW_HEIGHT = 480;
    private Matrix matrix = new Matrix();
    private int frameIndex = 0;
    private ModelViewer modelViewer;

    //    GLPoints mPoints;
//EGLUtils mEglUtils;
    Context context;
    private boolean isOpenHelmet = false;
    private int modelRootIndex;
    float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float mRatio;
    public static String modelPath = Environment.getExternalStorageDirectory()
            + File.separator + "ZeuseesFaceTracking";

    public MyGLRenderer(Context context, MyGLSurfaceView myGLSurfaceView) {
        this.context = context;
        mGLSurfaceView = myGLSurfaceView;
        InitModelFiles();
        mPaint = new Paint();
        mPaint.setColor(Color.rgb(57, 138, 243));
        int strokeWidth = Math.max(PREVIEW_HEIGHT / 240, 2);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.FILL);


        mRatio = (float) PREVIEW_HEIGHT / PREVIEW_WIDTH;
        android.opengl.Matrix.setIdentityM(mProjectionMatrix, 0);
        android.opengl.Matrix.setIdentityM(mViewMatrix, 0);
        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
        android.opengl.Matrix.setIdentityM(mMVPMatrix, 0);
        //投影转换矩阵
        android.opengl.Matrix.frustumM(mProjectionMatrix, 0, -mRatio, mRatio, -1.0f, 1.0f, 6.0f, 18.0f);
        //视见转换矩阵
        android.opengl.Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 12.0f, 0.0f, 0.0f, -4.0f, 0.0f, 1.0f, 0.0f);
    }

    /**
     * Surface 创建时 回调
     *
     * @param gl     1.0 api遗留参数
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        frameIndex = 0;
        mCameraHelper = new CameraHelper((Activity) mGLSurfaceView.getContext(),
                Camera.CameraInfo.CAMERA_FACING_FRONT, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        mCameraHelper.setPreviewCallback(this);
        // 准备摄像头绘制的画布
        mTextureID = new int[1];
        glGenTextures(mTextureID.length, mTextureID, 0);
        mSurfaceTexture = new SurfaceTexture(mTextureID[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mCameraFilter = new CameraFilter(mGLSurfaceView.getContext());
        mScreenFilter = new ScreenFilter(mGLSurfaceView.getContext());

        EGLContext eglContext = EGL14.eglGetCurrentContext();

        mMediaRecorder = new MyMediaRecorder(480, 640,
                "/sdcard/test_" + System.currentTimeMillis() + ".mp4", eglContext,
                mGLSurfaceView.getContext());


        mNv21Data = new byte[mCameraHelper.getmWidth() * mCameraHelper.getmHeight() * 2];
        mTmpBuffer = new byte[mCameraHelper.getmWidth() * mCameraHelper.getmHeight() * 2];
    }


    /**
     * Surface 发生改变回调
     *
     * @param gl
     * @param width  720   OpenGL相关的
     * @param height 1022  OpenGL相关的
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        mCameraHelper.startPreview(mSurfaceTexture);
        mCameraFilter.onReady(width, height);
        mScreenFilter.onReady(width, height);
        matrix.setScale(width / (float) PREVIEW_HEIGHT, height / (float) PREVIEW_WIDTH);
    }

    /**
     * 绘制一帧图像时 回调
     * 注意：该方法中一定要进行绘制操作
     * 该方法返回后，会交换渲染缓冲区，如果不绘制任何东西，会导致屏幕闪烁
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        glClearColor(255, 0, 0, 0);//屏幕清理颜色 红色
        // mask
        // GL_COLOR_BUFFER_BIT 颜色缓冲区
        // GL_DEPTH_BUFFER_BIT 深度
        // GL_STENCIL_BUFFER_BIT 模型
        glClear(GL_COLOR_BUFFER_BIT);

        // 绘制摄像头数据
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mtx);

        mCameraFilter.setMatrix(mtx);
        // mCameraFilter： 摄像头数据先输出到 fbo
        int textureId = mCameraFilter.onDrawFrame(mTextureID[0]);

        // TODO 【大眼相关代码】
        // TODO textureId = 大眼Filter.onDrawFrame(textureId);
        if (null != mBigEyeFilter) {
            mBigEyeFilter.setFace(mFaceTracking.getFace());
            textureId = mBigEyeFilter.onDrawFrame(textureId);
        }

        // TODO 【贴纸相关代码】
        if (null != mStickFilter) {
            mStickFilter.setFace(mFaceTracking.getFace()); // 需要定位人脸，所以需要 JavaBean
            textureId = mStickFilter.onDrawFrame(textureId);
        }

        // TODO 【美颜相关代码】
        if (null != mBeautyFilter) { // 没有不需要 人脸追踪/人脸关键点，整个屏幕美颜
            textureId = mBeautyFilter.onDrawFrame(textureId);
        }

        // TODO 【关键点】
//        if (null != mDemoFilter) {
//            mDemoFilter.setFace(mFaceTrack.getFace());
//            textureId = mDemoFilter.onDrawFrame(textureId);
//        }

        // TODO 【瘦脸】
        if (null != thinFaceFilter) {
            thinFaceFilter.setFace(mFaceTracking.getFace());
            textureId = thinFaceFilter.onDrawFrame(textureId);
        }

        // TODO 【亮眼】
        if (null != brightEyeFilter) {
            brightEyeFilter.setFace(mFaceTracking.getFace());
            textureId = brightEyeFilter.onDrawFrame(textureId);
        }

        // TODO 【口红】
        if (null != lipstickFilter) {
            lipstickFilter.setFace(mFaceTracking.getFace());
            textureId = lipstickFilter.onDrawFrame(textureId);
        }

        // TODO 【美颜02】
        if (null != glImageBeautyFilter) { // 没有不需要 人脸追踪/人脸关键点，整个屏幕美颜
            textureId = glImageBeautyFilter.onDrawFrame(textureId);
        }

        // TODO 【猫】
        if (null != catFilter) {
            catFilter.setFace(mFaceTracking.getFace());
            textureId = catFilter.onDrawFrame(textureId);
        }

        // textureId = xxxFilter.onDrawFrame(textureId);
        // ... textureId == 大眼后的纹理ID
        mScreenFilter.onDrawFrame(textureId);

        // 录制
        mMediaRecorder.encodeFrame(textureId, mSurfaceTexture.getTimestamp());
    }

    /**
     * surfaceTexture 画布有一个有效的新图像时 回调
     *
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mGLSurfaceView.requestRender();
    }

    public void surfaceDestroyed() {
        mCameraHelper.stopPreview();
    }

    /**
     * 开始录制
     *
     * @param speed
     */
    public void startRecording(float speed) {
        Log.e("MyGLRender", "startRecording");
        try {
            mMediaRecorder.start(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        Log.e("MyGLRender", "stopRecording");
        mMediaRecorder.stop();
    }

    /**
     * 开启大眼特效
     *
     * @param isChecked
     */
    public void enableBigEye(final boolean isChecked) {
        // BigEyeFilter bigEyeFilter = new BigEyeFilter(); // 这样可以吗  不行，必须在EGL线程里面绘制

        mGLSurfaceView.queueEvent(new Runnable() { // 把大眼渲染代码，加入到， GLSurfaceView 的 内置EGL 的 GLTHread里面
            public void run() {
                if (isChecked) {
                    mBigEyeFilter = new BigEyeFilter22222(mGLSurfaceView.getContext());
                    mBigEyeFilter.onReady(mWidth, mHeight);
                } else {
                    mBigEyeFilter.release();
                    mBigEyeFilter = null;
                }
            }
        });
    }

    // Camera画面只有有数据，就会回调此函数
    @Override // 要把相机的数据，给C++层做人脸追踪
    public void onPreviewFrame(final byte[] data, Camera camera) {
        final Camera.Size previewSize = camera.getParameters().getPreviewSize();
        synchronized (lockObj) {
            System.arraycopy(data, 0, mNv21Data, 0, data.length);
        }
        mHandler.post(() -> {
//                mFaceTrack.detector(mNv21Data);
            if (frameIndex == 0) {
                mFaceTracking.FaceTrackingInit(mNv21Data, PREVIEW_HEIGHT, PREVIEW_WIDTH);
            } else {
                mFaceTracking.Update(mNv21Data, PREVIEW_HEIGHT, PREVIEW_WIDTH, new FaceTracking.ExpressionListener() {
                    @Override
                    public void expressionType(int type) {
                        ToastUtil.showToast(context, "张嘴");
                    }
                });
            }
            frameIndex += 1;

            Log.v("camera-Orientation", mCameraHelper.getOrientation() + "");
            boolean rotate270 = mCameraHelper.getOrientation() == 270;
            List<Face> faceActions = mFaceTracking.getTrackingInfo();
//            Log.e("faceActions", faceActions.toString());


//            if (!mOverlap.getHolder().getSurface().isValid()) {
//                return;
//            }
//            Canvas canvas = mOverlap.getHolder().lockCanvas();
//            if (canvas == null)
//                return;
//            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
//            canvas.setMatrix(getMatrix());

            if (faceActions.size() == 1) {
                Face face = faceActions.get(0);
                Log.e("viewport:face", face.toString());
                if (isOpenHelmet) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (mOverlap == null) {
                            mOverlap = ((MainActivity) context).getmOverlap();
                        }
                        if (modelViewer == null) {
                            modelViewer = ((MainActivity) context).getModelView();
                        }
                        mOverlap.setVisibility(View.VISIBLE);
                        int left = face.left * mWidth / PREVIEW_HEIGHT;
                        //这里bottom为靠近底部的距离，而不是坐标.
                        int bottom = (PREVIEW_WIDTH - face.bottom) * mHeight / PREVIEW_WIDTH;
                        int faceWidth = face.width * mWidth / PREVIEW_HEIGHT;
                        int faceHeight = face.height * mHeight / PREVIEW_WIDTH;
                        Log.e("viewport:face111", mWidth + "  " + mHeight);
                        Log.e("viewport:face222", left + "  " + bottom + "  " + faceWidth + "   " + faceHeight);
//                        modelViewer.transformToUnitCube(new Float3(face.left + faceWidth / 2, face.bottom -faceHeight / 2, -4));
                    });

//                    Viewport viewport = new Viewport(left - 100, bottom - 550, faceWidth + 200, (int) (3.2 * faceHeight));
//                    Viewport viewport = new Viewport(left - 200, bottom - 500, faceWidth + 200, faceHeight + 500);
//                    modelViewer.getView().setViewport(viewport);


//                    if (modelRootIndex != 0) {
////                        MatrixOperation.eulerAnglesToRotationMatrix(face.eulerAngles);
////                        Mat3 transform =  MatrixOperation.eulerAnglesToRotationMatrix(face.eulerAngles);
////                        Log.v("isoen transform:", transform.toFloatArray().toString());
////                        modelViewer.getEngine().getTransformManager().setTransform(modelRootIndex,transform.toFloatArray());
//
//                        float pitchAngle = face.eulerAngles[0];
//                        float yawAngle = -face.eulerAngles[1];
//                        float rollAngle = -face.eulerAngles[2];
//                        if (Math.abs(yawAngle) > 50) {
//                            yawAngle = (yawAngle / Math.abs(yawAngle)) * 50;
//                        }
//                        if (Math.abs(pitchAngle) > 30) {
//                            pitchAngle = (pitchAngle / Math.abs(pitchAngle)) * 30;
//                        }
//                        Log.v("欧拉角02:", "pitchAngle:" + pitchAngle + "yawAngle:" + yawAngle + "rollAngle:" + rollAngle);
//                        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
//                        android.opengl.Matrix.translateM(mModelMatrix, 0, 0, 0, 1);
//                        android.opengl.Matrix.scaleM(mModelMatrix, 0, updateRootTransform(), 0, 1, 1, 1);
//                        android.opengl.Matrix.rotateM(mModelMatrix, 0, rollAngle, 0, 0, 1);
//                        android.opengl.Matrix.rotateM(mModelMatrix, 0, yawAngle, 0, 1, 0);
//                        android.opengl.Matrix.rotateM(mModelMatrix, 0, pitchAngle, 1, 0, 0);
//
//
////                        modelViewer.getCamera().setProjection(mProjectionMatrix,18.0f);
//
////                        android.opengl.Matrix.setIdentityM(mMVPMatrix, 0);
////                        android.opengl.Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
////                        android.opengl.Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mModelMatrix, 0);
//
//                        TransformManager tm = modelViewer.getEngine().getTransformManager();
//                        tm.setTransform(tm.getInstance(modelViewer.getAsset().getRoot()), mModelMatrix);
//
//
//                        //方案二
////                        float mRatio = 0.75f;
////                        android.opengl.Matrix.setIdentityM(mProjectionMatrix, 0);
////                        android.opengl.Matrix.setIdentityM(mViewMatrix, 0);
////                        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
////                        //投影转换矩阵
////                        android.opengl.Matrix.frustumM(mProjectionMatrix, 0, -mRatio, mRatio, -1.0f, 1.0f, 6.0f, 18.0f);
////                        //视见转换矩阵
////                        android.opengl.Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
////                        android.opengl.Matrix.setRotateEulerM(mModelMatrix,0,face.eulerAngles[0],face.eulerAngles[1],face.eulerAngles[2]);
////                        modelViewer.getEngine().getTransformManager().setTransform(modelViewer.getAsset().getRoot(), mModelMatrix);
//                    }
                } else {
//                    modelViewer.clearRootTransform();
//                    modelViewer.getRenderer().getClearOptions().clear = true;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if(mOverlap !=null){
                            mOverlap.setVisibility(View.GONE);
                        }
                    });
                }

                float[] points = null;
                for (Face r : faceActions) {
                    points = new float[106 * 2];
//                        Rect rect = new Rect(mCameraHelper.getmHeight() - r.left, r.top, mCameraHelper.getmHeight() - r.right, r.bottom);
                    for (int i = 0; i < 106; i++) {
                        float x;
                        if (rotate270) {
                            x = r.landmarks[i * 2];
                        } else {
                            x = mCameraHelper.getmHeight() - r.landmarks[i * 2];
                        }
                        float y = r.landmarks[i * 2 + 1];
                        points[i * 2] = view2openglX(x, mCameraHelper.getmHeight());
                        points[i * 2 + 1] = view2openglY(y, mCameraHelper.getmWidth());
                    }
                    if (face != null) {
                        face.setGlLandmarks(points);
                        mFaceTracking.setmFace(face);
                    }
                    Rect rect = new Rect(mCameraHelper.getmHeight() - r.left, r.top, mCameraHelper.getmHeight() - r.right, r.bottom);
//                    PointF[] points02 = new PointF[3];
//                    for (int i = 0; i < 3; i++) {
////                        points02[i] = new PointF(r.landmarks[i * 2], r.landmarks[i * 2 + 1]);
//                        if (i == 0) {
//                            points02[0] = new PointF(r.landmarks[20] - 5, r.landmarks[1] - 50);
//                        } else if (i == 1) {
//                            points02[1] = new PointF(r.landmarks[30] + 5, r.landmarks[1] - 50);
//                        } else if (i == 2) {
//                            points02[2] = new PointF((r.landmarks[20] + r.landmarks[0]) / 2, (r.landmarks[21] + r.landmarks[1]) / 2);
//                        }
//                    }
//                    float[] visibles = new float[3];
//
//                    for (int i = 0; i < points02.length; i++) {
//                        visibles[i] = 1.0f;
//                        if (rotate270) {
//                            points02[i].x = mCameraHelper.getmHeight() - points02[i].x;
//                        }
//                    }

//                    STUtils.drawFaceRect(canvas, rect, mCameraHelper.getmHeight(),
//                            mCameraHelper.getmWidth(), true);

//
//                    PointF[] points02 = getBrightEyeVertices(r.landmarks);
//                    float[] visibles = new float[16];
//                    for (int i = 0; i < points02.length; i++) {
//                        visibles[i] = 1.0f;
//                        if (rotate270) {
//                            points02[i].x = mCameraHelper.getmHeight() - points02[i].x;
//                        }
//                    }

//                    PointF[] points02 = getLipVertices(r.landmarks);
//                    float[] visibles = new float[20];
//                    for (int i = 0; i < points02.length; i++) {
//                        visibles[i] = 1.0f;
//                        if (rotate270) {
//                            points02[i].x = mCameraHelper.getmHeight() - points02[i].x;
//                        }
//                    }
//                    STUtils.drawPoints(canvas, mPaint, points02, visibles, mCameraHelper.getmHeight(),
//                            mCameraHelper.getmWidth(), true);
                }
            } else{
                mFaceTracking.setmFace(null);
//                Viewport viewport = new Viewport(0, 0, 0, 0);
//                modelViewer.getView().setViewport(viewport);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(mOverlap !=null){
                        mOverlap.setVisibility(View.GONE);
                    }
                });
            }
//            mOverlap.getHolder().unlockCanvasAndPost(canvas);
        });
    }

    private float view2openglX(float x, int width) {
        return Math.abs((x / width));
    }

    private float view2openglY(float y, int height) {
        return Math.abs(y / height);
    }


    // TODO 下面是 NDK OpenGL 53节课新增点

    /**
     * TODO 开启贴纸
     *
     * @param isChecked checkbox复选框是否勾上了
     */
    public void enableStick(final boolean isChecked) {
        mGLSurfaceView.queueEvent(new Runnable() { // 在EGL线程里面绘制 贴纸工作
            public void run() {
                if (isChecked) {
                    mStickFilter = new StickFilter(mGLSurfaceView.getContext());
                    mStickFilter.onReady(mWidth, mHeight);
                } else {
                    mStickFilter.release();
                    mStickFilter = null;
                }
            }
        });
    }


    public void enableDemo(final boolean isChecked) {
//        mGLSurfaceView.queueEvent(new Runnable() { // 在EGL线程里面绘制 贴纸工作
//            public void run() {
//                if (isChecked) {
//                    mDemoFilter = new DemoFilter(mGLSurfaceView.getContext());
//                    mDemoFilter.onReady(mWidth, mHeight);
//                } else {
////                    mDemoFilter.release();
//                    mDemoFilter = null;
//                }
//            }
//        });
    }

    /**
     * TODO 开启美颜
     *
     * @param isChecked checkbox复选框是否勾上了
     */
    public void enableBeauty(final boolean isChecked) {
        mGLSurfaceView.queueEvent(new Runnable() {
            public void run() {
                if (isChecked) {
                    mBeautyFilter = new BeautyFilter(mGLSurfaceView.getContext());
                    mBeautyFilter.onReady(mWidth, mHeight);
                } else {
                    mBeautyFilter.release();
                    mBeautyFilter = null;
                }
            }
        });
    }

    void InitModelFiles() {
        String assetPath = "ZeuseesFaceTracking";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        copyFilesFromAssets(mGLSurfaceView.getContext(), assetPath, sdcardPath);
        mFaceTracking = new FaceTracking(modelPath + "/models");
        mHandlerThread = new HandlerThread("DrawFacePointsThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }


    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir()) {
                    Log.d("mkdir", "can't make folder");
                }

                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public void enableThinFace(boolean isChecked) {
        mGLSurfaceView.queueEvent(new Runnable() {
            public void run() {
                if (isChecked) {
                    thinFaceFilter = new ThinFaceFilter(mGLSurfaceView.getContext());
                    thinFaceFilter.onReady(mWidth, mHeight);
                } else {
                    thinFaceFilter.release();
                    thinFaceFilter = null;
                }
            }
        });
    }

    public void enableBrightEye(boolean isChecked) {
        mGLSurfaceView.queueEvent(new Runnable() {
            public void run() {
                if (isChecked) {
                    brightEyeFilter = new BrightEyeFilter(mGLSurfaceView.getContext());
                    brightEyeFilter.onReady(mWidth, mHeight);
                } else {
                    brightEyeFilter.release();
                    brightEyeFilter = null;
                }
            }
        });
    }

    public void enableLipstick(boolean isChecked, DynamicMakeup makeup) {
        mGLSurfaceView.queueEvent(new Runnable() {
            public void run() {
                if (isChecked) {

                    String folderPath = MakeupHelper.getMakeupDirectory(context) + File.separator +
                            MakeupHelper.getMakeupList().get(1).unzipFolder;
                    DynamicMakeup makeup = null;
                    try {
                        makeup = ResourceJsonCodec.decodeMakeupData(folderPath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    lipstickFilter = new LipstickFilter(mGLSurfaceView.getContext(), makeup);
                    lipstickFilter.onReady(mWidth, mHeight);
                } else {
                    lipstickFilter.release();
                    lipstickFilter = null;
                }
            }
        });
    }

    public void enableBeauty02(boolean isChecked) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (isChecked) {
                    glImageBeautyFilter = new GLImageBeautyFilter(mGLSurfaceView.getContext());
                    glImageBeautyFilter.onReady(mWidth, mHeight);
                } else {
                    glImageBeautyFilter.release();
                    glImageBeautyFilter = null;
                }
            }
        });
    }

    public void enableCatstick(boolean isChecked, DynamicMakeup makeup) {
        mGLSurfaceView.queueEvent(new Runnable() {
            public void run() {
                if (isChecked) {
                    List<ResourceData> mResourceList = ResourceHelper.getResourceList();
                    ResourceData resourceData = mResourceList.get(0);
                    String unzipFolder = resourceData.unzipFolder;
                    String folderPath = ResourceHelper.getResourceDirectory(context) + File.separator + unzipFolder;
                    DynamicSticker sticker = null;
                    try {
                        sticker = ResourceJsonCodec.decodeStickerData(folderPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (sticker != null) {
                        catFilter = new CatFilter(mGLSurfaceView.getContext(), sticker);
                        catFilter.onReady(mWidth, mHeight);
                    }
                } else {
                    catFilter.release();
                    catFilter = null;
                }
            }
        });
    }

    public void enableHelmet(boolean openHelmet) {
        isOpenHelmet = openHelmet;
    }

    public void setModelRootIndex(int modelRootIndex) {
        this.modelRootIndex = modelRootIndex;
    }

    private Float3 centerPoint = new Float3(0.0f, 0.0f, -4.0f);

    private float[] updateRootTransform() {
        FilamentAsset filamentAsset = modelViewer.getAsset();
        float[] tempArray = filamentAsset.getBoundingBox().getCenter();
        Float3 center = new Float3(tempArray[0], tempArray[1], tempArray[2]);
        float[] tempArray02 = filamentAsset.getBoundingBox().getHalfExtent();
        Float3 halfExtent = new Float3(tempArray02[0], tempArray02[1], tempArray02[2]);
        float maxExtent = 2.0f * max(halfExtent);
        float scaleFactor = 2.0f / maxExtent;
        Float3 tempArray03 = new Float3(centerPoint.getX() / scaleFactor, centerPoint.getY() / scaleFactor, centerPoint.getZ() / scaleFactor);

        center = new Float3(center.getX() - tempArray03.getX(), center.getY() - tempArray03.getY(), center.getZ() - tempArray03.getZ());
        //[0.99999976, 0.0, 0.0, 0.0,
        // 0.0, 0.99999976, 0.0, 0.0,
        // 0.0, 0.0, 0.99999976, 0.0,
        // 0.002481579, -1.0520217E-5, -3.812845, 1.0]
        return MatrixKt.transpose(MatrixKt.scale(new Float3(scaleFactor)).times(MatrixKt.translation(center.unaryMinus()))).toFloatArray();
    }
}

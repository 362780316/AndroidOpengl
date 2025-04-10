package com.ray.opengl;

import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.ray.opengl.resource.MakeupHelper;
import com.ray.opengl.resource.ResourceHelper;

public class Camera3DActivity extends AppCompatActivity {

    private SurfaceView mGLSurfaceView;
    public SurfaceView mOverlap = null;
    CheckBox chk_beauty02;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity3d_camera);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initResources();
        mGLSurfaceView = findViewById(R.id.glSurfaceView);

        mOverlap = (SurfaceView) findViewById(R.id.surfaceViewOverlap);
        mOverlap.setZOrderOnTop(true);
        mOverlap.getHolder().setFormat(PixelFormat.TRANSLUCENT);


        Point point = new Point();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(point);

        int width = point.x;
        int height = point.y;

        System.out.println("MainActivity手机的分辨率:" + width + "~~~~" + height);

//        ((MyRecordButton) findViewById(R.id.btn_record)).setOnRecordListener(
//                new MyRecordButton.OnRecordListener() {
//                    /**
//                     * 开始录制
//                     */
//                    @Override
//                    public void onStartRecording() {
//                        mGLSurfaceView.startRecording();
//                    }
//
//                    /**
//                     * 停止录制
//                     */
//                    @Override
//                    public void onStopRecording() {
//                        mGLSurfaceView.stopRecording();
//                        Toast.makeText(Camera3DActivity.this, "录制完成！", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//        ((RadioGroup) findViewById(R.id.group_record_speed)).setOnCheckedChangeListener(
//                new RadioGroup.OnCheckedChangeListener() {
//                    /**
//                     * 选择录制模式
//                     * @param group
//                     * @param checkedId
//                     */
//                    @Override
//                    public void onCheckedChanged(RadioGroup group, int checkedId) {
//                        if (checkedId == R.id.rbtn_record_speed_extra_slow) { //极慢
//                            mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_EXTRA_SLOW);
//                        } else if (checkedId == R.id.rbtn_record_speed_slow) {   //慢
//                            mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_SLOW);
//                        } else if (checkedId == R.id.rbtn_record_speed_normal) { //正常
//                            mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_NORMAL);
//                        } else if (checkedId == R.id.rbtn_record_speed_fast) {   //快
//                            mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_FAST);
//                        } else if (checkedId == R.id.rbtn_record_speed_extra_fast) { //极快
//                            mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_EXTRA_FAST);
//                        }
//                    }
//                });
//
//        ((CheckBox) findViewById(R.id.chk_bigeye)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableBigEye(isChecked));
//        // TODO 下面是 NDK OpenGL 53节课新增点
//        ((CheckBox) findViewById(R.id.chk_stick)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableStick(isChecked));
//        ((CheckBox) findViewById(R.id.chk_beauty)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableBeauty(isChecked));
//        ((CheckBox) findViewById(R.id.chk_demo)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableDemo(isChecked));
//        ((CheckBox) findViewById(R.id.chk_thinface)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableThinFace(isChecked));
//        ((CheckBox) findViewById(R.id.chk_brighteye)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableBrightEye(isChecked));
//        ((CheckBox) findViewById(R.id.chk_lipstick)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                String folderPath = MakeupHelper.getMakeupDirectory(Camera3DActivity.this) + File.separator +
//                        MakeupHelper.getMakeupList().get(1).unzipFolder;
//                DynamicMakeup makeup = null;
//                try {
//                    makeup = ResourceJsonCodec.decodeMakeupData(folderPath);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                mGLSurfaceView.enableLipstick(isChecked, makeup);
//            }
//        });
//
//        chk_beauty02 = ((CheckBox) findViewById(R.id.chk_beauty02));
//        chk_beauty02.setChecked(true);
//        mGLSurfaceView.post(() -> mGLSurfaceView.enableBeauty02(true));
//
//        chk_beauty02.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                mGLSurfaceView.enableBeauty02(isChecked);
//            }
//        });

//        ((CheckBox) findViewById(R.id.chk_cat)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableCatstick(isChecked, null));
//        ((CheckBox) findViewById(R.id.chk_helmet)).setOnCheckedChangeListener((buttonView, isChecked) -> {
//            mGLSurfaceView.enableCatstick(isChecked, null);
//            modelViewer = ModelViewer(mGLSurfaceView);
//        });


    }

//    ModelViewer modelViewer;
    /**
     * 初始化动态贴纸、滤镜等资源
     */
    private void initResources() {
        new Thread(() -> {
            ResourceHelper.initAssetsResource(Camera3DActivity.this);
//            FilterHelper.initAssetsFilter(MainActivity.this);
            MakeupHelper.initAssetsMakeup(Camera3DActivity.this);
        }).start();
    }

    public SurfaceView getmOverlap() {
        return mOverlap;
    }
}

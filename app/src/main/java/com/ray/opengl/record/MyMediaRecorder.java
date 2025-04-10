package com.ray.opengl.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.opengl.EGLContext;

public class MyMediaRecorder {

    private final int mWidth;
    private final String mOutputPath;
    private final EGLContext mEglContext;
    private final Context mContext;
    private MediaCodec mMediaCodec;
    private final int mHeight;
    private Surface mInputSurface;
    private MediaMuxer mMediaMuxer;
    private Handler mHandler;
    private MyEGL mEGL;
    private boolean isStart;
    private int index;
    private float mSpeed;
    private long lastTimeUs;

    public MyMediaRecorder(int width, int height, String outputPath, EGLContext eglContext,
                           Context context) {
        mWidth = width;
        mHeight = height;
        mOutputPath = outputPath;
        mEglContext = eglContext;
        mContext = context;
    }

    public void start(float speed) throws IOException {
        mSpeed = speed;
        /**
         * 1， 创建 MediaCodec 编码器
         * type: 哪种类型的视频编码器
         * MIMETYPE_VIDEO_AVC： H.264
         */
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        /**
         * 2，配置编码器参数
         */
        //视频格式
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                mWidth, mHeight);
        //设置码率
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        //帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        //颜色格式 （从Surface中自适应）
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //关键帧间隔
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20);

        //配置编码器
        mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        /**
         * 3， 创建输入 Surface（虚拟屏幕）
         */
        mInputSurface = mMediaCodec.createInputSurface();

        /**
         * 4, 创建封装器
         */

        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        /**
         * 5, 配置 EGL 环境
         */
        HandlerThread handlerThread = new HandlerThread("MyMediaRecorder");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        mHandler = new Handler(looper);
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                mEGL = new MyEGL(mEglContext, mInputSurface, mContext, mWidth, mHeight);
                mMediaCodec.start();//启动编码器
                isStart = true;
            }
        });
    }

    /**
     * 停止录制
     */
    public void stop() {
        isStart = false;
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getEncodedData(true);

                    if (mMediaCodec != null) {
                        mMediaCodec.stop();
                        mMediaCodec.release();
                        mMediaCodec = null;
                    }
                    if (mMediaMuxer != null) {
                        try {
                            mMediaMuxer.stop();
                            mMediaMuxer.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mMediaMuxer = null;
                    }
                    if (mInputSurface != null) {
                        mInputSurface.release();
                        mInputSurface = null;
                    }
                    mEGL.release();
                    mEGL = null;
                    mHandler.getLooper().quitSafely();
                    mHandler = null;
                }
            });
        }
    }

    public void encodeFrame(final int textureId, final long timestamp) {
        if (!isStart) {
            return;
        }
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //画到虚拟屏幕
                    if (null != mEGL) {
                        mEGL.draw(textureId, timestamp);
                    }
                    //从编码器中取数据
                    getEncodedData(false);
                }
            });
        }
    }

    /**
     * 获取编码后的数据
     */
    private void getEncodedData(boolean endOfStream) {
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
        }
        //输出缓冲区
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            int status = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);//10ms
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //endOfStream = true, 要录制，继续循环，继续取新的编码数据
                if (!endOfStream) {
                    break;
                }
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat outputFormat = mMediaCodec.getOutputFormat();
                index = mMediaMuxer.addTrack(outputFormat);
                mMediaMuxer.start();//启动封装器
            } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            } else {
                //成功取到一个有效数据
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(status);
                if (null == outputBuffer) {
                    throw new RuntimeException("getOutputBuffer fail");
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    //如果是配置信息
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    //除以大于1的 ： 加速
                    //小于1 的： 减速
                    bufferInfo.presentationTimeUs = (long)(bufferInfo.presentationTimeUs / mSpeed);

                    // 可能会出现类似：TimeUs < lastTimeUs xxxxxx for video Track
                    if(bufferInfo.presentationTimeUs <= lastTimeUs){
                        bufferInfo.presentationTimeUs = (long)(lastTimeUs + 1_000_000 /25/mSpeed);
                    }
                    lastTimeUs = bufferInfo.presentationTimeUs;

                    //偏移位置
                    outputBuffer.position(bufferInfo.offset);
                    //可读写的总产度
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    try {
                        //写数据
                        mMediaMuxer.writeSampleData(index, outputBuffer, bufferInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //释放输出缓冲区
                mMediaCodec.releaseOutputBuffer(status, false);

                // 编码结束
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }//end while
    }
}

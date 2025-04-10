package com.ray.opengl.utils;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;

import androidx.camera.core.ImageProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class Utils {

    static byte[] data;
    static byte[] rowData;

    public static byte[] getDataFromImage(ImageProxy image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        int size = width * height * ImageFormat.getBitsPerPixel(format) / 8;
        if (data == null || data.length != size) {
            data = new byte[size];
        }
        if (rowData == null || rowData.length != planes[0].getRowStride()){
            rowData = new byte[planes[0].getRowStride()];
        }
        int channelOffset = 0;  //偏移
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0; // y 从0开始
                    break;
                case 1:
                    channelOffset = width * height; // u 开始
                    break;
                case 2:
                    channelOffset = (int) (width * height * 1.25); // v开始 w*h+ w*h/4（u数据长度）
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride(); //行跨度 每行的数据量
            int pixelStride = planes[i].getPixelStride(); // 像素跨度 ,uv的存储间隔
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift; // u与v只有一半
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            int length;
            for (int row = 0; row < h; row++) {
                if (pixelStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset++] = rowData[col * pixelStride];
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }


    /**
     * YUV_420_888转NV21
     *
     * @param image CameraX ImageProxy
     * @return byte array
     */
    public static byte[] yuv420ToNv21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        int size = image.getWidth() * image.getHeight();
        byte[] nv21 = new byte[size * 3 / 2];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);

        byte[] u = new byte[uSize];
        uBuffer.get(u);

        //每隔开一位替换V，达到VU交替
        int pos = ySize + 1;
        for (int i = 0; i < uSize; i++) {
            if (i % 2 == 0) {
                nv21[pos] = u[i];
                pos += 2;
            }
        }
        return nv21;
    }

    public static String copyAsset2Dir(Context context, String name) {
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        if (!cascadeFile.exists()) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = context.getAssets().open(name);
                fos = new FileOutputStream(cascadeFile);
                int len;
                byte[] buffer = new byte[2048];

                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is == null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fos == null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return cascadeFile.getAbsolutePath();
    }
}

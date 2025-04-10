package com.ray.opengl;

import androidx.camera.core.CameraX;

public interface MyPreviewCallback {
    void onPreviewFrame(byte[] data, CameraX camera, int rotationDegrees);
}

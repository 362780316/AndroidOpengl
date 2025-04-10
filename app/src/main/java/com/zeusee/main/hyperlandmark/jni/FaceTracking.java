package com.zeusee.main.hyperlandmark.jni;

import android.util.Log;

import com.ray.opengl.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;


public class FaceTracking {

    static {
        System.loadLibrary("zeuseesTracking-lib");
//        System.loadLibrary("native-lib");
    }

    //    public native static void update(byte[] data, int height, int width, long session);
    public native static void update(byte[] data, int height, int width, int angle, boolean mirror, long session);

    //    public native static void initTracking(byte[] data, int height, int width, long session);
    public native static void initTracker(int height, int width, int scale, long session);

    public native static long createSession(String modelPath);

    public native static void releaseSession(long session);

    public native static int getTrackingNum(long session);

    public native static int[] getTrackingLandmarkByIndex(int index, long session);

    public native static int[] getTrackingLocationByIndex(int index, long session);

    public native static int getTrackingIDByIndex(int index, long session);

    public native static int[] getAttributeByIndex(int index, long session);

    public native static float[] getEulerAngleByIndex(int index, long session);

    private long session;
    private List<Face> faces;
    Face face;

    public FaceTracking(String pathModel) {
        Log.d("FaceTracking", pathModel + "");
        session = createSession(pathModel);
        faces = new ArrayList<Face>();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        releaseSession(session);
    }

    public void FaceTrackingInit(byte[] data, int height, int width) {
//        initTracking(data, height, width, session);
        initTracker(height, width, 1, session);
    }

    public void Update(byte[] data, int height, int width, ExpressionListener expressionListener) {
        update(data, height, width, 270, true, session);
        int numsFace = getTrackingNum(session);
        faces.clear();
        Log.d("numsFace_tracking", numsFace + "");
//        Face face = null;
        if (numsFace == 1) {
            if (expressionListener != null) {
                int[] attributes = getAttributeByIndex(0, session);
                for (int j = 0; j < attributes.length; j++) {
                    Log.v("ExpressionListener", String.valueOf(attributes[j]));
                    if (j == 0) {
                        if (attributes[0] == 1) {
                            Log.v("ExpressionListenerattri", String.valueOf(attributes[0]));
                            expressionListener.expressionType(attributes[0]);
                        }
                    }
                }
                Log.v("ExpressionListener", "===========");
            }
        }
        for (int i = 0; i < numsFace; i++) {
            int[] landmarks = getTrackingLandmarkByIndex(i, session);
            int[] faceRect = getTrackingLocationByIndex(i, session);
            float[] eulerAngles = getEulerAngleByIndex(i, session);
            int[] attributes = getAttributeByIndex(i, session);

            //默认0,0,1,0,      1.是否张嘴   2..  3..  4.是否睁眼
//            for (int j = 0; j < attributes.length; j++) {
//                Log.v("第" + j + "个attributesitem", String.valueOf(attributes[j]));
//            }

            Log.v("faceRect", faceRect.length + "," + faceRect[0] + "," + faceRect[1] + "," + faceRect[2] + "," + faceRect[3]);
            Log.v("eulerAngles", eulerAngles.length + "," + eulerAngles[0] + eulerAngles[1] + eulerAngles[2]);
            int id = getTrackingIDByIndex(i, session);
            Face face = new Face(faceRect[0], faceRect[1], faceRect[2], faceRect[3], landmarks, id, eulerAngles);
            faces.add(face);
        }
    }

    public interface ExpressionListener {
        void expressionType(int type);
    }

    public List<Face> getTrackingInfo() {
        return faces;
    }

    public Face getFace() {
        return face;
    }

    public void setmFace(Face face) {
        this.face = face;
    }
}

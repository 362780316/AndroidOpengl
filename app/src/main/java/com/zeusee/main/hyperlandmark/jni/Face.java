package com.zeusee.main.hyperlandmark.jni;


import java.util.Arrays;

public class Face {
    Face(int x1, int y1, int x2, int y2) {
        left = x1;
        top = y1;
        right = x2;
        bottom = y2;
        height = y2 - y1;
        width = x2 - x1;
        landmarks = new int[106 * 2];
    }


    Face(int x1, int y1, int _width, int _height, int[] landmark, int id,float[] eulerAngles) {
        left = x1;
        top = y1;
        right = x1 + _width;
        bottom = y1 + _height;
        width = _width;
        height = _height;
        landmarks = landmark;
        ID = id;
        this.eulerAngles = eulerAngles;
    }


    public int ID;

    public int left;
    public int top;
    public int right;
    public int bottom;
    public int height;
    public int width;
    public int[] landmarks;
    public float [] glLandmarks;

    public float[] eulerAngles;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int[] getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(int[] landmarks) {
        this.landmarks = landmarks;
    }

    public float[] getGlLandmarks() {
        return glLandmarks;
    }

    public void setGlLandmarks(float[] glLandmarks) {
        this.glLandmarks = glLandmarks;
    }

    public float[] getEulerAngles() {
        return eulerAngles;
    }

    public void setEulerAngles(float[] eulerAngles) {
        this.eulerAngles = eulerAngles;
    }

    @Override
    public String toString() {
        return "Face{" +
                "ID=" + ID +
                ", left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", height=" + height +
                ", width=" + width +
                ", landmarks=" + Arrays.toString(landmarks) +
                ", glLandmarks=" + Arrays.toString(glLandmarks) +
                ", eulerAngles=" + Arrays.toString(eulerAngles) +
                '}';
    }
}

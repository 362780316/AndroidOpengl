package com.ray.opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glBufferSubData;
import static android.opengl.GLES20.glDeleteBuffers;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by Ray on 2018/11/29.
 */

public class GLPoints {
    private FloatBuffer vertexBuffer;
    private int bufferLength = 106 * 2 * 4;
    private int programId = -1;
    private int aPositionHandle;

    private int[] vertexBuffers;


    private String fragmentShader =
            "void main() {\n" +
                    "    gl_FragColor = vec4(1.0,0.0,0.0,1.0);\n" +
                    "}";
    private String vertexShader = "attribute vec2 aPosition;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(aPosition,0.0,1.0);\n" +
            "    gl_PointSize = 10.0;\n" +
            "}";

    public GLPoints() {
        vertexBuffer = ByteBuffer.allocateDirect(bufferLength)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.position(0);
        initPoints();
    }

    public void initPoints() {
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = glGetAttribLocation(programId, "aPosition");

        vertexBuffers = new int[1];
        glGenBuffers(1, vertexBuffers, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffers[0]);
        glBufferData(GL_ARRAY_BUFFER, bufferLength, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void setPoints(float[] points) {
        vertexBuffer.rewind();
        vertexBuffer.put(points);
        vertexBuffer.position(0);
    }


    public void drawPoints() {
        glUseProgram(programId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffers[0]);
        glBufferSubData(GL_ARRAY_BUFFER, 0, bufferLength, vertexBuffer);
        glEnableVertexAttribArray(aPositionHandle);
        glVertexAttribPointer(aPositionHandle, 2, GL_FLOAT, false,
                0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDrawArrays(GL_POINTS, 0, 106);
    }

    public void release() {
        glDeleteProgram(programId);
        glDeleteBuffers(1, vertexBuffers, 0);
    }
}

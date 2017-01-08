package com.example.gaolf.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * Created by gaolf on 17/1/7.
 */

public class CameraPreviewGLSurfaceView extends GLSurfaceView implements ICameraPreview {

    private static final String VERTEX_SHADER =
            "attribute vec4 inPosition;\n" +
            "attribute vec2 inTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "\tgl_Position = vec4(inPosition);\n" +
            "\ttexCoord = inTexCoord;\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying vec2 texCoord;\n" +
            "uniform samplerExternalOES tex;\n" +
            "void main() {\n" +
            "\tgl_FragColor = texture2D(tex, texCoord);\n" +
            "}";

    private static final float[] VERTEX_DATA = new float[] {
        -1.0f, 1.0f, 0.0f,      // 左上
        0.0f, 1.0f,
        -1.0f, -1.0f, 0.0f,
        1.0f, 1.0f,
        1.0f, 1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        1.0f, 0.0f,
    };


    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8d65;

    private static final String TAG = "CameraPreviewGLSV";

    private Camera mCamera;
    private int mCameraId;
    private int mSurfaceTextureName;
    private SurfaceTexture mSurfaceTexture;
    private Handler handler = new Handler(Looper.getMainLooper());

    public CameraPreviewGLSurfaceView(Context context) {
        super(context);
    }

    @Override
    public void setCamera(Camera camera, int id) {
        mCamera = camera;
        mCameraId = id;

        if (mCamera != null && mCameraId >= 0) {
            try {
                init();
            } catch (Exception e) {
                Log.e(TAG, "init failed");
                e.printStackTrace();
            }
        } else {
            mSurfaceTexture.release();
        }

    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(new MyRenderer());
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private class MyRenderer implements Renderer {

        private static final int IN_POSITION = 0;
        private static final int IN_TEX_COORD = 1;

        private int program;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            compileProgram();
            bindVertexAttrib();
            bindTexture();

            mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                private int count = 0;
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    Log.d(TAG, "onFrameAvailable, " + count++);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                requestRender();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });

            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        private void compileProgram() {

            int vShader = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vShader, VERTEX_SHADER);
            glCompileShader(vShader);
            String infoLog = glGetShaderInfoLog(vShader);
            if (!TextUtils.isEmpty(infoLog)) {
                Log.e(TAG, "vShader compile info log: " + infoLog);
            }

            int fShader = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fShader, FRAGMENT_SHADER);
            glCompileShader(fShader);
            infoLog = glGetShaderInfoLog(fShader);
            if (!TextUtils.isEmpty(infoLog)) {
                Log.e(TAG, "fShader compile info log: " + infoLog);
            }

            program = glCreateProgram();
            glAttachShader(program, vShader);
            glAttachShader(program, fShader);
            glLinkProgram(program);
            infoLog = glGetProgramInfoLog(program);
            if (!TextUtils.isEmpty(infoLog)) {
                Log.e(TAG, "program link info log: " + infoLog);
            }
            glUseProgram(program);
        }

        private void bindVertexAttrib() {
            int[] attribLocArray = new int[2];
            int[] arrayBuffer = new int[1];

            attribLocArray[IN_POSITION] = glGetAttribLocation(program, "inPosition");
            attribLocArray[IN_TEX_COORD] = glGetAttribLocation(program, "inTexCoord");
            Log.d(TAG, "inPositionLoc: " + attribLocArray[IN_POSITION] +
                    ", inTexCoordLoc: " + attribLocArray[IN_TEX_COORD]);

            FloatBuffer vertexDataBuffer = ByteBuffer.allocateDirect(Float.SIZE / 8 * VERTEX_DATA.length)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            vertexDataBuffer.put(VERTEX_DATA).position(0);
            glGenBuffers(1, arrayBuffer, 0);
            glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer[0]);
            glBufferData(GL_ARRAY_BUFFER, Float.SIZE / 8 * VERTEX_DATA.length, vertexDataBuffer, GL_STATIC_DRAW);

            glVertexAttribPointer(attribLocArray[IN_POSITION], 3, GL_FLOAT, false, Float.SIZE / 8 * 5, 0);
            glVertexAttribPointer(attribLocArray[IN_TEX_COORD], 2, GL_FLOAT, false, Float.SIZE / 8 * 5, Float.SIZE / 8 * 3);
            glEnableVertexAttribArray(attribLocArray[IN_POSITION]);
            glEnableVertexAttribArray(attribLocArray[IN_TEX_COORD]);
        }

        private void bindTexture() {
            int texUniLoc = -1;
            int[] textures = new int[1];

            glActiveTexture(GL_TEXTURE0);
            glGenTextures(1, textures, 0);
            mSurfaceTextureName = textures[0];
            glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
            mSurfaceTexture = new SurfaceTexture(mSurfaceTextureName);
            glGetUniformLocation(program, "tex");
            glUniform1i(texUniLoc, 0);
        }


        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (mCamera == null) {
                return;
            }


            glViewport(0, 0, width, height);
            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here
            Camera.Parameters parameters = mCamera.getParameters();
            CameraUtil.adjustCameraParameters(parameters);
            CameraUtil.setCameraDisplayOrientation((Activity) getContext(), mCameraId, mCamera);
            mCamera.setParameters(parameters);

            // start preview with new settings
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            try {
                mSurfaceTexture.updateTexImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            glClear(GL_COLOR_BUFFER_BIT);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }
    }
}

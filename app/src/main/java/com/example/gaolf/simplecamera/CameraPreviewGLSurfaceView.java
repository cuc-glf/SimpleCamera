package com.example.gaolf.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gaolf on 17/1/7.
 */

public class CameraPreviewGLSurfaceView extends GLSurfaceView implements ICameraPreview {

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

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mSurfaceTextureName = textures[0];
            mSurfaceTexture = new SurfaceTexture(mSurfaceTextureName);
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

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (mCamera == null) {
                return;
            }

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
        }
    }
}

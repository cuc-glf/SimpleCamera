package com.example.gaolf.simplecamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.gaolf.simplecamera.shader.GaussianShader;
import com.example.gaolf.simplecamera.shader.IShader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by gaolf on 17/1/7.
 */

public class CameraPreviewGLSurfaceView extends GLSurfaceView implements ICameraPreview {

    private static final String TAG = "CameraPreviewGLSV";

    private Camera mCamera;
    private int mCameraId;

    private IShader mShader;
    private Handler handler = new Handler(Looper.getMainLooper());
    private SurfaceTexture mSurfaceTexture;

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
            mShader.destroy();
            mShader = null;
            mSurfaceTexture.release();
            mSurfaceTexture = null;
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
            if (mShader != null) {
                Log.e(TAG, "onSurfaceCreated, mShader is not null, need to clear before stop.");
            }
            mShader = new GaussianShader();
            mShader.init();
            mShader.apply();
            mSurfaceTexture = new SurfaceTexture(mShader.getTextureName());
            mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            requestRender();
                        }
                    });
                }
            });
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }


        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (mCamera == null) {
                Log.e(TAG, "onSurfaceChanged, mCamera is null");
                return;
            }
            if (mShader == null) {
                Log.e(TAG, "onSurfaceChanged, mShader is null");
                return;
            }
            if (mSurfaceTexture == null) {
                Log.e(TAG, "onSurfaceChanged, mSurfaceTexture is null");
                return;
            }

            mShader.onSizeChanged(width, height);
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
            CameraUtil.adjustCameraParameters(parameters, mCameraId, null);
//            CameraUtil.setCameraDisplayOrientation((Activity) getContext(), mCameraId, mCamera);
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
            if (mShader == null) {
                Log.e(TAG, "onDrawFrame, mShader is null");
                return;
            }
            if (mSurfaceTexture == null) {
                Log.e(TAG, "onDrawFrame, mSurfaceTexture is null");
                return;
            }
            try {
                mSurfaceTexture.updateTexImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mShader.draw();
        }
    }
}

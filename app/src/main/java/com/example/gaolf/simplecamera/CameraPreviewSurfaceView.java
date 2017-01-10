package com.example.gaolf.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by gaolf on 17/1/5.
 */

public class CameraPreviewSurfaceView extends SurfaceView implements ICameraPreview, SurfaceHolder.Callback {

    private static final String TAG = "CameraPreviewSV";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;

    public CameraPreviewSurfaceView(Context context) {
        super(context);
    }

    @Override
    public void setCamera(Camera camera, int id) {
        mCamera = camera;
        mCameraId = id;

        if (mCamera != null && mCameraId >= 0) {
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

    }

    public void surfaceCreated(SurfaceHolder holder) {

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera == null) {
            return;
        }
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
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
        CameraUtil.adjustCameraParameters(parameters, mCameraId, null);
        CameraUtil.setCameraDisplayOrientation((Activity) getContext(), mCameraId, mCamera);
        mCamera.setParameters(parameters);


        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

}

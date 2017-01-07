package com.example.gaolf.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by gaolf on 17/1/5.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;

    public CameraPreview(Context context, Camera camera, int cameraId) {
        super(context);
        mCamera = camera;
        mCameraId = cameraId;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
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
        adjustCameraParameters(parameters);
        setCameraDisplayOrientation((Activity) getContext(), mCameraId, mCamera);
        mCamera.setParameters(parameters);


        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    private void adjustCameraParameters(Camera.Parameters parameters) {

        // 1. 设置尺寸
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        long max = 0;
        long temp = 0;
        Camera.Size bestSize = null;
        for (Camera.Size size : supportedSizes) {
            temp = size.height * size.width;
            if (temp > max) {
                max = temp;
                bestSize = size;
            }
        }

        if (bestSize != null) {
            parameters.setPreviewSize(bestSize.width, bestSize.height);
        }

        // 2. 设置对焦方式
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes == null || supportedFocusModes.isEmpty()) {
            Log.e(TAG, "no supported focus mode");
            return;
        }
        final List<String> focusModePriority = new ArrayList<>();
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);     // 最优先
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_AUTO);
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_EDOF);
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_INFINITY);
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_FIXED);
        focusModePriority.add(Camera.Parameters.FOCUS_MODE_MACRO);

        for (String focusMode : supportedFocusModes) {
            if (findStringInStringList(focusMode, supportedFocusModes) < 0) {
                Log.e(TAG, "unknown supported focus mode: " + focusMode);
                return;
            }
        }

        Collections.sort(supportedFocusModes, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                int lPri = findStringInStringList(lhs, focusModePriority);
                int rPri = findStringInStringList(rhs, focusModePriority);
                return lPri - rPri;
            }
        });

        String bestFocusMode = supportedFocusModes.get(0);
        Log.d(TAG, "best focus mode: " + bestFocusMode);
        parameters.setFocusMode(bestFocusMode);
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private int findStringInStringList(String string, List<String> list) {
        String temp;
        for (int i = 0; i < list.size(); i++) {
            temp = list.get(i);
            if (TextUtils.equals(string, temp)) {
                return i;
            }
        }

        return -1;
    }
}

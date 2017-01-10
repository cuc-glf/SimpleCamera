package com.example.gaolf.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by gaolf on 17/1/5.
 */

public class CameraUtil implements ICameraUtil {

    private static final String TAG = "CameraUtil";

    private static Context getApplication() {
        return MyApplication.getApplication();
    }

    public boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    public static void adjustCameraParameters(Camera.Parameters parameters, int cameraId, Point previewSize) {
        if (previewSize == null) {
            previewSize = new Point();
            Context c = getApplication();
            WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getSize(previewSize);
        }
        float previewAspect = (float)previewSize.x / previewSize.y;    // aspect ＝ width / height

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        // 1. 设置尺寸
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        float currAspect;
        long max = 0;
        long temp = 0;
        Camera.Size bestSize = null;
        for (Camera.Size size : supportedSizes) {
            temp = size.height * size.width;
            currAspect = (cameraInfo.orientation == 0 || cameraInfo.orientation == 180) ?
                    (float)size.width / size.height :
                    (float)size.height / size.width;
            if (
                    temp > max  // 分辨率更高
                    && Math.abs(currAspect - previewAspect) < 0.1f  // 宽高比合适
                )
            {
                max = temp;
                bestSize = size;
            }
        }

        if (bestSize != null) {
            Log.d(TAG, "bestSize: " + bestSize.width + "x" + bestSize.height);
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


    public static int findStringInStringList(String string, List<String> list) {
        String temp;
        for (int i = 0; i < list.size(); i++) {
            temp = list.get(i);
            if (TextUtils.equals(string, temp)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 当渲染到纹理时，该设置似乎没用，Camera只会按照原来的方向来渲染纹理
     */
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

}

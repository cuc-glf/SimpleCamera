package com.example.gaolf.simplecamera;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by gaolf on 17/1/5.
 */

public class CameraUtil implements ICameraUtil {

    public boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }



}

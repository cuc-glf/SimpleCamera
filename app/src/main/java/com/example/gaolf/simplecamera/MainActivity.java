package com.example.gaolf.simplecamera;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by gaolf on 17/1/5.
 */

public class MainActivity extends Activity {

    private CameraContainerView cameraContainerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        cameraContainerView = (CameraContainerView) findViewById(R.id.main_activity_camera);
    }

    @Override
    protected void onResume() {
        super.onStart();
        // 打开Camera
        cameraContainerView.onShow();

    }

    @Override
    protected void onPause() {
        super.onPause();
        // 关闭Camera
        cameraContainerView.onHide();
    }
}
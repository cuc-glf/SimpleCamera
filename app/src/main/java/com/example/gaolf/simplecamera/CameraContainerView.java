package com.example.gaolf.simplecamera;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.getNumberOfCameras;

/**
 * Created by gaolf on 17/1/5.
 */

public class CameraContainerView extends RelativeLayout {

    public static final int CAMERA_PREVIEW_TYPE_SURFACE_VIEW = 0;
    public static final int CAMERA_PREVIEW_TYPE_GL_SURFACE_VIEW = 1;

    private int mCameraPreviewType = CAMERA_PREVIEW_TYPE_GL_SURFACE_VIEW;

    private static final String TAG = "CameraContainerView";

    private ICameraUtil cameraUtil = new CameraUtil();
    private ICameraPreview cameraPreview;
    private ViewGroup previewContainer;
    private Camera camera;

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            File pictureFile = getOutputPictureFile();
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            Toast.makeText(getContext(), "照片已保存：" + pictureFile.getPath(), Toast.LENGTH_LONG).show();

        }

        private File getOutputPictureFile() {
            File mediaDir = new File(Environment.getExternalStorageDirectory(), "camera");
            if (!mediaDir.exists()) {
                if (!mediaDir.mkdirs()) {
                    return null;
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File pictureFile = new File(mediaDir, timeStamp + ".jpg");
            if (pictureFile.exists()) {
                return null;
            }
            return pictureFile;
        }
    };

    public CameraContainerView(Context context) {
        super(context);
        init();
    }

    public CameraContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCameraPreviewType(int cameraPreviewType) {
        mCameraPreviewType = cameraPreviewType;
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.camera_container_view, this);
        findViewById(R.id.camera_container_view_take_picture_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, pictureCallback);
            }
        });

        previewContainer = (ViewGroup)findViewById(R.id.camera_container_view_preview_container);
    }

    public void onShow() {
        boolean hasCamera = cameraUtil.checkCameraHardware(getContext());
        if (!hasCamera) {
            Log.e("CameraView", "no camera");
            return;
        }
        CameraAndId cameraAndId = getCameraInstance();
        if (cameraAndId == null) {
            Log.e("CameraView", "camera open failed");
            return;
        }
        camera = cameraAndId.camera;
        cameraPreview = createCameraPreview(getContext(), camera, cameraAndId.id);
        previewContainer.removeAllViews();
        if (cameraPreview == null) {
            Log.e(TAG, "fail creating camera preview");
            return;
        }
        previewContainer.addView((View) cameraPreview, 0);

    }

    private ICameraPreview createCameraPreview(Context context, Camera camera, int id) {
        ICameraPreview cameraPreview = null;
        switch (mCameraPreviewType) {
            case CAMERA_PREVIEW_TYPE_GL_SURFACE_VIEW:
                CameraPreviewGLSurfaceView cameraPreviewGLSurfaceView = new CameraPreviewGLSurfaceView(context);
                cameraPreviewGLSurfaceView.setCamera(camera, id);
                cameraPreview = cameraPreviewGLSurfaceView;
                break;
            case CAMERA_PREVIEW_TYPE_SURFACE_VIEW:
                CameraPreviewSurfaceView cameraPreviewSurfaceView = new CameraPreviewSurfaceView(context);
                cameraPreviewSurfaceView.setCamera(camera, id);
                cameraPreview = cameraPreviewSurfaceView;
                break;
        }
        return cameraPreview;
    }

    public void onHide() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        if (cameraPreview != null) {
            cameraPreview.setCamera(null, -1);
            cameraPreview = null;
        }
    }

    private static CameraAndId getCameraInstance() {
        Camera c = null;
        int id = -1;
        try {
            int numberOfCameras = getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    c = Camera.open(i);
                    id = i;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (c != null && id != -1) {
            CameraAndId cameraAndId = new CameraAndId();
            cameraAndId.id = id;
            cameraAndId.camera = c;
            return cameraAndId;
        } else {
            return null;
        }
    }

    private static class CameraAndId {
        public Camera camera;
        public int id;
    }


}

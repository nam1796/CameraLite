package com.intelligorithm.cameralite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

/**
 * Created by rescobar on 01/08/2016.
 */

public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener{

    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    private SurfaceTexture surfaceTexture;
    private Handler uiHandler;


    private String TAG = "CameraLite";

    public void setmCamera(Camera mCamera) {
        this.mCamera = mCamera;
        if (mCamera == null) {
            return;
        }
        startCameraPreview();
    }

    public Camera getmCamera() {
        return mCamera;
    }

    public CameraTextureView(Context context, Camera mCamera) {
        super(context);

        this.setSurfaceTextureListener(this);

        this.mCamera = mCamera;
        this.surfaceTexture = getSurfaceTexture();
        this.uiHandler = new Handler();






        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }

        float ratio;
        if(mPreviewSize.height >= mPreviewSize.width)
            ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
        else
            ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;

        setMeasuredDimension(width, (int) (width * ratio));
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    void startCameraPreview() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    mCamera.setPreviewTexture(getSurfaceTexture());
                } catch (IOException e) {
                    e.printStackTrace();
                }


                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);


                List<String> focusModes = parameters.getSupportedFocusModes();

                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                parameters.setRotation(90);
                mCamera.setParameters(parameters);
                mCamera.setDisplayOrientation(90);


                mCamera.startPreview();

            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        startCameraPreview();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {









        return true;

    }




}

package com.glass.cuxtomcam;

import java.io.IOException;
import java.util.List;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback, OnZoomChangeListener {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Context mContext;
	private static String TAG = "CAMERA PREVIEW";
	private int zoomOffset;
	private CameraListener mCallback;

	public CameraPreview(Context context, Camera camera) {
		super(context);
		mContext = context;
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);

	}

	public void setCameraListener(CameraListener listener) {
		mCallback = listener;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
			Log.e(TAG, "Error Stopping camera preview--> " + e.getMessage());
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			Parameters mParameters = mCamera.getParameters();
			List<Camera.Size> previewSizes = mParameters
					.getSupportedPreviewSizes();
			List<Camera.Size> pictureSizes = mParameters
					.getSupportedPictureSizes();
			List<int[]> fps = mParameters.getSupportedPreviewFpsRange();

			// // You need to choose the most appropriate previewSize for your
			// app
			// Camera.Size previewSize = previewSizes.get(1);// .... select one
			// of
			// // previewSizes here

			Camera.Size picturesize = pictureSizes.get(0);
			mParameters.setPreviewSize(previewSizes.get(2).width,
					previewSizes.get(2).height);
			mParameters.setPictureSize(picturesize.width, picturesize.height);
			mParameters.setPreviewFpsRange(fps.get(2)[0], fps.get(2)[1]);
			mParameters.setRotation(90);
			mCamera.setParameters(mParameters);
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
			if (mCallback != null)
				mCallback.onCameraInit();
		} catch (Exception e) {
			Log.e(TAG, "Error starting camera preview--> " + e.getMessage());
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			CameraUtils.setCameraDisplayOrientation(mContext, 0, mCamera);
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
			if (mCamera.getParameters().isZoomSupported()) {
				mCamera.setZoomChangeListener(this);
				zoomOffset = mCamera.getParameters().getMaxZoom() / 5;
			}
		} catch (IOException e) {
			Log.e("Error starting preview", e.getMessage());
		}

	}

	public void startCamera() {
		if (mCamera != null) {
			mCamera.startPreview();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
			// release the camera
			mCamera.release();
			// unbind the camera from this object
			mCamera = null;
		}

	}

	public synchronized void zoomIn() {
		if (mCamera != null && mCamera.getParameters().isZoomSupported()
				&& mCamera.getParameters().isSmoothZoomSupported()) {
			int zoomvalue = mCamera.getParameters().getZoom() + zoomOffset;
			if (zoomvalue <= mCamera.getParameters().getMaxZoom()) {
				mCamera.startSmoothZoom(zoomvalue);
			}
		} else {
			Toast.makeText(mContext, "Zoom is not supported", Toast.LENGTH_LONG)
					.show();
		}
	}

	public synchronized void zoomOut() {
		if (mCamera != null && mCamera.getParameters().isZoomSupported()
				&& mCamera.getParameters().isSmoothZoomSupported()) {
			int zoomvalue = mCamera.getParameters().getZoom() - zoomOffset;
			if (zoomvalue >= 0) {
				mCamera.startSmoothZoom(zoomvalue);
			}
		} else {
			Toast.makeText(mContext, "Zoom is not supported", Toast.LENGTH_LONG)
					.show();
		}
	}

	@Override
	public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {
		// Log.i("Camera Zoom Value", zoomValue + "");

	}

}

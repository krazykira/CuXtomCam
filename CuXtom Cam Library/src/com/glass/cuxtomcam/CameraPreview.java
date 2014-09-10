package com.glass.cuxtomcam;

import java.io.IOException;
import java.util.List;

import com.glass.cuxtomcam.constants.CuxtomIntent.CAMERA_MODE;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback, OnZoomChangeListener {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private static String TAG = "CAMERA PREVIEW";
	private int zoomOffset;
	private CameraListener mCallback;
	private int cameraMode;
	private boolean zooming;
	private Handler mHandler;

	public CameraPreview(Context context, Camera camera, int cameraMode,
			Handler mHandler) {
		super(context);
		this.mHandler = mHandler;
		mCamera = camera;
		mHolder = getHolder();
		this.cameraMode = cameraMode;
		mHolder.addCallback(this);
		mHolder.setKeepScreenOn(true);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		zooming = false;

	}

	public void setCameraListener(CameraListener listener) {
		mCallback = listener;
	}

	@Override
	public synchronized void surfaceChanged(SurfaceHolder holder, int format,
			int width, int height) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (cameraMode != CAMERA_MODE.PHOTO_MODE
				&& mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		} else if (holder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		if (cameraMode == CAMERA_MODE.PHOTO_MODE) {

			try {
				mCamera.stopPreview();
				mCamera.setPreviewDisplay(mHolder);
			} catch (Exception e) {
				Log.e(TAG + " Error starting camera preview--> ",
						e.getMessage());
			}
			new Thread(new Runnable() {

				@Override
				public void run() {
					// stop preview before making changes

					Parameters mParameters = mCamera.getParameters();
					List<Camera.Size> pictureSizes = mParameters
							.getSupportedPictureSizes();
					List<int[]> fps = mParameters.getSupportedPreviewFpsRange();

					// List<Camera.Size> previewSizes = mParameters
					// .getSupportedPreviewSizes();
					// // You need to choose the most appropriate previewSize
					// for
					// your
					// app
					// Camera.Size previewSize = previewSizes.get(1);// ....
					// select
					// one
					// of
					// // previewSizes here
					// This is the default resolution of glass 640 x 360. It is
					// also
					// available in the preview size list. This should only be
					// set
					// when we want to take a picture otherwise we will use the
					// default preview
					// mParameters.setPreviewSize(640, 360);
					Camera.Size picturesize = pictureSizes.get(0);
					mParameters.setPictureSize(picturesize.width,
							picturesize.height);
//					Uncomment if there is issue during preview
//					mParameters.setPreviewFpsRange(fps.get(5)[0], fps.get(5)[1]);
					// mParameters.setRotation(90);
					onOrientationChanged(mParameters,
							Configuration.ORIENTATION_LANDSCAPE);
					mCamera.setParameters(mParameters);
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							mCamera.startPreview();

						}
					});

				}
			}).start();

		}
		if (mCallback != null)
			mCallback.onCameraInit();
	}

	/**
	 * Possible Picture Orientation fix
	 * 
	 * @param param
	 * @param orientation
	 */
	public void onOrientationChanged(Parameters param, int orientation) {
		if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
			return;
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(0, info);
		orientation = (orientation + 45) / 90 * 90;
		int rotation = 0;
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			rotation = (info.orientation - orientation + 360) % 360;
		} else { // back-facing camera
			rotation = (info.orientation + orientation) % 360;
		}
		param.setRotation(rotation);
	}

	@Override
	public synchronized void surfaceCreated(SurfaceHolder holder) {
		if (mCamera != null) {
			try {
				// CameraUtils.setCameraDisplayOrientation(mContext, 0,
				// mCamera);
				// onOrientationChanged(mCamera.getParameters(),
				// Configuration.ORIENTATION_LANDSCAPE);
				if (mCamera.getParameters().isZoomSupported()) {
					mCamera.setZoomChangeListener(this);
					zoomOffset = mCamera.getParameters().getMaxZoom() / 4;
				}
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
				Log.e("Error starting preview", e.getMessage());
			}

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
		mHolder = null;

	}

	public synchronized void zoomIn() {
		if (mCamera != null && mCamera.getParameters().isZoomSupported()
				&& mCamera.getParameters().isSmoothZoomSupported() && !zooming) {
			int zoomvalue = mCamera.getParameters().getZoom() + zoomOffset;
			if (zoomvalue <= mCamera.getParameters().getMaxZoom()) {
				mCamera.startSmoothZoom(zoomvalue);
			}
		}
	}

	public synchronized void zoomOut() {
		if (mCamera != null && mCamera.getParameters().isZoomSupported()
				&& mCamera.getParameters().isSmoothZoomSupported() && !zooming) {
			int zoomvalue = mCamera.getParameters().getZoom() - zoomOffset;
			if (zoomvalue >= 0) {
				mCamera.startSmoothZoom(zoomvalue);
			}
		}
	}

	@Override
	public synchronized void onZoomChange(int zoomValue, boolean stopped,
			Camera camera) {
		// Log.i("Camera Zoom Value", zoomValue + "");
		zooming = !stopped;

	}

}

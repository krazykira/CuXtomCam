package com.glass.cuxtomcam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;

import com.glass.cuxtomcam.constants.CuxtomIntent;
import com.glass.cuxtomcam.constants.CuxtomIntent.CAMERA_MODE;
import com.glass.cuxtomcam.constants.CuxtomIntent.FILE_TYPE;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;

public class CuxtomCamActivity extends Activity implements BaseListener,
		CameraListener {

	private final String TAG = "CAMERA ACTIVITY";
	private final int KEY_SWIPE_DOWN = 4;
	private Camera mCamera;
	private CameraPreview mPreview;
	private FrameLayout previewCameraLayout;
	private GestureDetector mGestureDetector;
	private MediaRecorder recorder;

	// *****************************
	// these values are set by the calling activity
	// *****************************
	private int cameraMode;
	private String fileName;
	private String folderPath;
	private int video_duration;
	private boolean enablezoom;
	private File videofile;

	private Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(TAG, "Error in getCameraInstance--> " + e.getMessage());
		}
		return c; // returns null if camera is unavailable
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LoadExtras(getIntent());
		loadUI();

	}

	/**
	 * Load all the extra values that have been sent by the calling activity
	 * 
	 * @param intent
	 *            containing extras
	 */
	private void LoadExtras(Intent intent) {
		// check for folder path where pictures will be saved
		if (intent.hasExtra(CuxtomIntent.FOLDER_PATH)) {
			folderPath = intent.getStringExtra(CuxtomIntent.FOLDER_PATH);
		} else {
			folderPath = Environment.getExternalStorageDirectory()
					+ File.separator + Environment.DIRECTORY_PICTURES
					+ File.separator + "CuXtomCamera";
		}

		// Check for CameraMode
		if (intent.hasExtra(CuxtomIntent.CAMERA_MODE)) {
			cameraMode = intent.getIntExtra(CuxtomIntent.CAMERA_MODE,
					CAMERA_MODE.PHOTO_MODE);
		} else {
			cameraMode = CAMERA_MODE.PHOTO_MODE;
		}
		// Check for FileName
		if (intent.hasExtra(CuxtomIntent.FILE_NAME)) {
			fileName = intent.getStringExtra(CuxtomIntent.FILE_NAME);
		} else {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			if (cameraMode == CAMERA_MODE.PHOTO_MODE) {
				fileName = "pic" + timeStamp;
			} else {
				fileName = "vid" + timeStamp;
			}
		}
		// Check for video duration
		if (cameraMode == CAMERA_MODE.VIDEO_MODE
				&& intent.hasExtra(CuxtomIntent.VIDEO_DURATION)) {
			video_duration = intent.getIntExtra(CuxtomIntent.VIDEO_DURATION,
					3600);
		} else {
			video_duration = 3600;
		}
		// check whether zoom functionailty should be enabled
		if (intent.hasExtra(CuxtomIntent.ENABLE_ZOOM)) {
			enablezoom = intent.getBooleanExtra(CuxtomIntent.ENABLE_ZOOM, true);
		} else {
			enablezoom = true;
		}
	}

	/**
	 * Load UI according to the seetings
	 */
	private void loadUI() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		previewCameraLayout = new FrameLayout(this);
		previewCameraLayout.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		// Create an instance of Camera
		mCamera = getCameraInstance();
		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		mPreview.setCameraListener(this);
		previewCameraLayout.addView(mPreview);
		setContentView(previewCameraLayout);

		mGestureDetector = new GestureDetector(this);
		mGestureDetector.setBaseListener(this);

	}

	/**
	 * Events occurred by performing gestures on activity will be received here
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	/**
	 * Handle glass tap gestures
	 */

	@Override
	public boolean onGesture(Gesture g) {
		switch (g) {
		case TAP:
			if (cameraMode == CAMERA_MODE.PHOTO_MODE) {
				String s = mCamera.getParameters().getFocusMode();
				if (!s.equalsIgnoreCase("infinity")) {
					mCamera.autoFocus(new AutoFocusCallback() {

						@Override
						public void onAutoFocus(boolean success, Camera camera) {
							if (success)
								camera.takePicture(null, null, mPictureCallback);
						}
					});
				} else {
					mCamera.takePicture(null, null, mPictureCallback);
				}
			} else {
				Intent intent = new Intent();
				intent.putExtra(CuxtomIntent.FILE_PATH, videofile.getPath());
				intent.putExtra(CuxtomIntent.FILE_TYPE, FILE_TYPE.VIDEO);
				setResult(RESULT_OK, intent);
				finish();
			}
			return true;
		case SWIPE_RIGHT:
			if (enablezoom)
				mPreview.zoomIn();
			return true;
		case SWIPE_LEFT:
			if (enablezoom)
				mPreview.zoomOut();
			return true;
		default:
			return false;
		}
	}

	/**
	 * Ignore any key that is pressed
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA)
			return true;
		else
			return false;
	}

	/**
	 * Ignore swipe down event
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KEY_SWIPE_DOWN) {
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it
								// first
	}

	/**
	 * Save picture once its taken and send result to the calling activity
	 */
	private PictureCallback mPictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			try {
				// Directories can be created but they cannot be seen when you
				// connect to computer unless you access them from
				// ddms in eclipse. There is some sort of special viewing
				// permission on the glass directores for now
				File dir = new File(folderPath);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				File f = new File(dir, fileName + ".jpg");
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(data);
				fos.flush();
				fos.close();
				Intent intent = new Intent();
				intent.putExtra(CuxtomIntent.FILE_PATH, f.getPath());
				intent.putExtra(CuxtomIntent.FILE_TYPE, FILE_TYPE.PHOTO);
				setResult(RESULT_OK, intent);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found: " + e.getMessage());
				setResult(RESULT_CANCELED);
			} catch (IOException e) {
				Log.e(TAG, "Error accessing file: " + e.getMessage());
				setResult(RESULT_CANCELED);
			}
			finish();
		}
	};

	/**
	 * Start video recording
	 */
	private void startVideoRecorder() {
		// THIS IS NEEDED BECAUSE THE GLASS CURRENTLY THROWS AN ERROR OF
		// "MediaRecorder start failed: -19"
		// THIS WONT BE NEEDED INCASE OF PHONE AND TABLET
		try {
			mCamera.setPreviewDisplay(null);
		} catch (java.io.IOException ioe) {
			Log.d(TAG,
					"IOException nullifying preview display: "
							+ ioe.getMessage());
		}
		mCamera.stopPreview();
		mCamera.unlock();
		recorder = new MediaRecorder();
		// Let's initRecorder so we can record again
		initRecorder();
	}

	/**
	 * Initialize video recorder to record video
	 */
	private void initRecorder() {
		try {
			File dir = new File(folderPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File f = new File(dir, fileName + ".mp4");
			videofile = f;
			recorder.setCamera(mCamera);

			// Step 2: Set sources
			recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

			// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
			recorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_HIGH));

			// Step 4: Set output file
			recorder.setOutputFile(f.getAbsolutePath());
			// Step 5: Set the preview output
			recorder.setPreviewDisplay(mPreview.getHolder().getSurface());
			// Step 6: Prepare configured MediaRecorder
			recorder.setMaxDuration(video_duration * 1000);
			recorder.setOnInfoListener(new OnInfoListener() {
				
				@Override
				public void onInfo(MediaRecorder mr, int what, int extra) {
					if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
					{
						Intent intent = new Intent();
						intent.putExtra(CuxtomIntent.FILE_PATH, videofile.getPath());
						intent.putExtra(CuxtomIntent.FILE_TYPE, FILE_TYPE.VIDEO);
						setResult(RESULT_OK, intent);
						finish();
					}
					
				}
			});
			recorder.prepare();
			recorder.start();
		} catch (Exception e) {
			Log.e("Error Stating CuXtom Camera", e.getMessage());
		}
	}

	private void releaseMediaRecorder() {
		if (recorder != null) {
			recorder.reset(); // clear recorder configuration
			recorder.release(); // release the recorder object
			recorder = null;
			mCamera.lock(); // lock camera for later use
		}
	}


	@Override
	public void onCameraInit() {
		if (cameraMode == CAMERA_MODE.VIDEO_MODE) {
			startVideoRecorder();
		}

	}

}

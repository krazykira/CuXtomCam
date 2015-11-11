package com.glass.cuxtomcam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.glass.cuxtomcam.CameraOverlay.Mode;
import com.glass.cuxtomcam.VideoMerger.OnVideoListener;
import com.glass.cuxtomcam.constants.CuxtomIntent;
import com.glass.cuxtomcam.constants.CuxtomIntent.CAMERA_MODE;
import com.glass.cuxtomcam.constants.CuxtomIntent.FILE_TYPE;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

public class CuxtomCamActivity extends Activity implements BaseListener,
		CameraListener, MediaScannerConnection.OnScanCompletedListener {

	private final String TAG = "CuXtom Cam ACTIVITY";
	private final int KEY_SWIPE_DOWN = 4;
	private Camera mCamera;
	private CameraPreview mPreview;
	private RelativeLayout previewCameraLayout;
	private GestureDetector mGestureDetector;
	private MediaRecorder recorder;
	private TextView tv_recordingDuration;
	private int totalVideoDuration;
	private ScheduledExecutorService mExecutorService;
	// For multiple Video Files
	private ArrayList<File> videoFiles;
	private boolean hasMultipleVideoFiles = false;

	/*
	 * This is the Maximum Allowed video duration in Seconds
	 */
	private int maxAllowedVideoDuration = 1 * 60;
	// *****************************
	// these values are set by the calling activity
	// *****************************
	private final String DEFAULT_DIRECTORY = "CuXtom Cam";
	private final String VIDEO_DIRECTORY = "Videos";
	private final String PHOTO_DIRECTORY = "Photos";

	private int cameraMode;
	private String fileName;
	private String folderPath;
	private int video_duration;
	private boolean enablezoom;
	private File videofile;
	private CameraOverlay mOverlay;
	private SoundEffectPlayer mSoundEffects;
	private Runnable recordingTimer = new Runnable() {

		@Override
		public synchronized void run() {
			totalVideoDuration++;
			final int seconds = totalVideoDuration % 60;
			final int minutes = totalVideoDuration / 60;

			if (seconds < 10) {
				if (minutes < 10) {
					tv_recordingDuration.post(new Runnable() {

						@Override
						public void run() {
							tv_recordingDuration.setText("0" + minutes + ":0"
									+ seconds);

						}
					});
				} else {
					tv_recordingDuration.post(new Runnable() {

						@Override
						public void run() {
							tv_recordingDuration.setText(minutes + ":0"
									+ seconds);
						}
					});
				}

			} else {
				if (minutes < 10) {
					tv_recordingDuration.post(new Runnable() {

						@Override
						public void run() {
							tv_recordingDuration.setText("0" + minutes + ":"
									+ seconds);
						}
					});
				} else {
					tv_recordingDuration.post(new Runnable() {

						@Override
						public void run() {
							tv_recordingDuration.setText(minutes + ":"
									+ seconds);
						}
					});
				}
			}

		}
	};

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
		Log.e(TAG, "Oncreate()");
		mSoundEffects = new SoundEffectPlayer();
		mSoundEffects.setup(this);
		loadExtras(getIntent());
		Log.e(TAG, "Loading extras completed");
		loadUI();
		Log.e(TAG, "Loading UI completed");

	}

	@Override
	protected void onDestroy() {
		mSoundEffects.deconstruct();
		super.onDestroy();
	}

	/**
	 * Load all the extra values that have been sent by the calling activity
	 * 
	 * @param intent
	 *            containing extras
	 */
	private void loadExtras(Intent intent) {
		// Check for CameraMode
		if (intent.hasExtra(CuxtomIntent.CAMERA_MODE)) {
			cameraMode = intent.getIntExtra(CuxtomIntent.CAMERA_MODE,
					CAMERA_MODE.PHOTO_MODE);
		} else {
			cameraMode = CAMERA_MODE.PHOTO_MODE;
		}

		// check for folder path where pictures will be saved
		if (intent.hasExtra(CuxtomIntent.FOLDER_PATH)) {
			folderPath = intent.getStringExtra(CuxtomIntent.FOLDER_PATH);
		} else {
			folderPath = Environment.getExternalStorageDirectory()
					+ File.separator + Environment.DIRECTORY_PICTURES
					+ File.separator + DEFAULT_DIRECTORY;
			createSubDirectory();
		}

		// Check for FileName
		if (intent.hasExtra(CuxtomIntent.FILE_NAME)) {
			fileName = intent.getStringExtra(CuxtomIntent.FILE_NAME);
		} else {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			if (cameraMode == CAMERA_MODE.PHOTO_MODE) {
				fileName = "pic_" + timeStamp;
			} else {
				fileName = "vid_" + timeStamp;
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
	 * Create a sub directory to save photos and videos
	 */
	private void createSubDirectory() {

		if (cameraMode == CAMERA_MODE.VIDEO_MODE) {
			folderPath = folderPath + File.separator + VIDEO_DIRECTORY;
		} else {
			folderPath = folderPath + File.separator + PHOTO_DIRECTORY;
		}
	}

	/**
	 * Load UI according to the settings provided by calling activity
	 */
	private void loadUI() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		previewCameraLayout = new RelativeLayout(this);
		previewCameraLayout.setLayoutParams(new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT));
		// Create an instance of Camera
		mCamera = getCameraInstance();
		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera, cameraMode, new Handler());
		mPreview.setCameraListener(this);
		mOverlay = new CameraOverlay(this);
		previewCameraLayout.addView(mPreview);
		previewCameraLayout.addView(mOverlay);
		setContentView(previewCameraLayout);
		tv_recordingDuration = new TextView(this);
		mGestureDetector = new GestureDetector(this);
		mGestureDetector.setBaseListener(this);

	}

	/**
	 * initialize video recording UI with timer
	 */
	private void initVideoRecordingUI(String initializeTime) {
		LayoutParams rl_param = new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		rl_param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl_param.addRule(RelativeLayout.CENTER_HORIZONTAL);
		rl_param.addRule(RelativeLayout.ALIGN_BOTTOM, mPreview.getId());
		rl_param.setMargins(0, 0, 0, 30);
		tv_recordingDuration.setText(initializeTime);
		tv_recordingDuration.setTextSize(28);
		tv_recordingDuration.setLayoutParams(rl_param);
		previewCameraLayout.addView(tv_recordingDuration);
		mExecutorService = Executors.newSingleThreadScheduledExecutor();
		totalVideoDuration = 0;
		mExecutorService.scheduleAtFixedRate(recordingTimer, 1, 1,
				TimeUnit.SECONDS);
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
	 * Handle Glass Swipe and Tap Gestures
	 */

	@Override
	public synchronized boolean onGesture(Gesture g) {
		switch (g) {
		case TAP:
			if (cameraMode == CAMERA_MODE.PHOTO_MODE) {
				mOverlay.setMode(CameraOverlay.Mode.FOCUS);
				mCamera.takePicture(null, null, mPictureCallback);
				mSoundEffects.shutter();
			} else
				endVideoRecording();

			return true;
		case SWIPE_RIGHT:
			if (enablezoom)
				mPreview.zoomIn();
			return true;
		case TWO_SWIPE_RIGHT:
			if (enablezoom)
				mPreview.zoomIn();
			return true;
		case SWIPE_LEFT:
			if (enablezoom)
				mPreview.zoomOut();
			return true;
		case TWO_SWIPE_LEFT:
			if (enablezoom)
				mPreview.zoomOut();
			return true;
		case SWIPE_DOWN:
			onBackPressed();
			return true;
		default:
			return false;
		}
	}

	/**
	 * Ignore any key that is pressed. Just handle camera key
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			if (cameraMode == CAMERA_MODE.PHOTO_MODE) {
				mOverlay.setMode(CameraOverlay.Mode.FOCUS);
				mCamera.takePicture(null, null, mPictureCallback);
				mSoundEffects.shutter();
			}
			return true;
		} else
			return false;
	}

	/**
	 * Ignore swipe down event
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KEY_SWIPE_DOWN) {
			if (videofile != null) {
				// if you are using MediaRecorder, release it
				// first
				releaseMediaRecorder();
				mExecutorService.shutdown();
				mOverlay.setMode(Mode.PLAIN);
				if (videofile.exists())
					videofile.delete();
			}
			setResult(RESULT_CANCELED);
			onScanCompleted(null, null);
			super.onKeyUp(keyCode, event);
		}
		return false;
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

				mOverlay.setMode(CameraOverlay.Mode.PLAIN);
				Intent intent = new Intent();
				intent.putExtra(CuxtomIntent.FILE_PATH, f.getPath());
				intent.putExtra(CuxtomIntent.FILE_TYPE, FILE_TYPE.PHOTO);
				setResult(RESULT_OK, intent);
				// initiate media scan and put the new things into the path
				// array to
				// make the scanner aware of the location and the files you want
				// to
				// see
				MediaScannerConnection.scanFile(getApplicationContext(),
						new String[] { f.getPath() }, null,
						CuxtomCamActivity.this);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found: " + e.getMessage());
				setResult(RESULT_CANCELED);
			} catch (IOException e) {
				Log.e(TAG, "Error accessing file: " + e.getMessage());
				setResult(RESULT_CANCELED);
			}
		}
	};

	/**
	 * Initialize Video Recorder and start configuring recorder for single or
	 * Multiple files
	 */
	private void initVideoRecorder() {
		recorder = new MediaRecorder();
		// Let's initRecorder
		if (video_duration > maxAllowedVideoDuration) {
			// Configure Multiple Video Files Recorder
			videoFiles = new ArrayList<File>();
			int remainingVideoDuration = video_duration
					- maxAllowedVideoDuration;
			// Video Duration that is left to be recorded
			video_duration = remainingVideoDuration;
			startMultipleVideoRecorder(maxAllowedVideoDuration);
			hasMultipleVideoFiles = true;
		} else
			// Configure Single Video Files Recorder
			startSingleVideoRecorder();
	}

	/**
	 * Configure video recorder to record a single video file. There will be no
	 * need to merge this file.
	 */
	private void startSingleVideoRecorder() {

		File dir = new File(folderPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		videofile = new File(dir, fileName + ".mp4");
		recorder.setCamera(mCamera);

		// Step 2: Set sources
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		// Step 4: Set output file
		recorder.setOutputFile(videofile.getAbsolutePath());
		// Step 5: Set the preview output
		recorder.setPreviewDisplay(mPreview.getHolder().getSurface());
		// Step 6: Prepare configured MediaRecorder
		recorder.setMaxDuration(0);
		recorder.setMaxFileSize(0);
		recorder.setOnErrorListener(new OnErrorListener() {

			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				Log.e("Error Recording", what + " Extra " + extra);

			}
		});
		recorder.setOnInfoListener(new OnInfoListener() {

			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					endVideoRecording();
				}

			}
		});
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				try {
					mSoundEffects.camcorder();
					recorder.prepare();
					recorder.start();
					mOverlay.setMode(Mode.RECORDING);

				} catch (Exception e) {
					if (e != null && e.getMessage() != null)
						Log.e("Error Starting CuXtom Camera for video recording",
								e.getMessage());
				}

			}
		});

	}

	/**
	 * Configure video recorder Start recording videos that will be merged later
	 * onwards, once all videos have been recorded
	 */
	private void startMultipleVideoRecorder(int currentRecordingDuration) {

		File dir = new File(folderPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File vFile = new File(dir, System.currentTimeMillis() + ".mp4");
		videoFiles.add(vFile);
		recorder.setCamera(mCamera);

		// Step 2: Set sources
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		// Step 4: Set output file
		recorder.setOutputFile(vFile.getAbsolutePath());
		// Step 5: Set the preview output
		recorder.setPreviewDisplay(mPreview.getHolder().getSurface());
		// Step 6: Prepare configured MediaRecorder
		recorder.setMaxDuration(currentRecordingDuration * 1000);
		recorder.setMaxFileSize(-1);
		recorder.setOnErrorListener(new OnErrorListener() {

			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				Log.e("Error Recording", what + " Extra " + extra);

			}
		});
		recorder.setOnInfoListener(new OnInfoListener() {

			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					resumeOrEndVideoRecording();
				}

			}
		});
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				try {
					mSoundEffects.camcorder();
					recorder.prepare();
					recorder.start();
					mOverlay.setMode(Mode.RECORDING);

				} catch (Exception e) {
					if (e != null && e.getMessage() != null)
						Log.e("Error Starting CuXtom Camera for video recording",
								e.getMessage());
				}

			}
		});

	}

	/**
	 * End Video Recording, Refresh the Disk and End the activity
	 */
	private void endVideoRecording() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (hasMultipleVideoFiles) {
					releaseMediaRecorder();
					mSoundEffects.camcorderStop();
					mExecutorService.shutdown();
					mOverlay.setMode(Mode.PLAIN);
					// Merge video files into 1 file
					mergeVideoIntoOneFile();
				} else {
					releaseMediaRecorder();
					mSoundEffects.camcorderStop();
					mExecutorService.shutdown();
					mOverlay.setMode(Mode.PLAIN);
					Intent intent = new Intent();
					intent.putExtra(CuxtomIntent.FILE_PATH, videofile.getPath());
					intent.putExtra(CuxtomIntent.FILE_TYPE, FILE_TYPE.VIDEO);
					setResult(RESULT_OK, intent);
					/*
					 * initiate media scan and put the new things into the path
					 * array to make the scanner aware of the location and the
					 * files you want to see
					 */MediaScannerConnection.scanFile(getApplicationContext(),
							new String[] { videofile.getPath() }, null,
							CuxtomCamActivity.this);
				}

			}
		});
	}

	/**
	 * Resume or End video recording depending upon whether the video has been
	 * recorded for the whole duration or not
	 */
	private void resumeOrEndVideoRecording() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// The Whole video has been recorded successfully
				if (video_duration <= 0)
					endVideoRecording();
				else {
					if (recorder != null) {
						try {
							recorder.stop();
							recorder.reset();
						} catch (Exception e) {
							Log.e("error stopping", e.getMessage());
						}

					}
//					mCamera.stopPreview();
//					mCamera.unlock();
					totalVideoDuration = video_duration
							/ maxAllowedVideoDuration;
					
					if (video_duration < maxAllowedVideoDuration)
						startMultipleVideoRecorder(video_duration);
					else {
						int remainingVideoDuration = video_duration
								- maxAllowedVideoDuration;
						// Video Duration that is left to be recorded
						video_duration = remainingVideoDuration;
						// Start recording the another video file
						startMultipleVideoRecorder(maxAllowedVideoDuration);
					}
				}
			}
		});

	}

	/**
	 * Start merging the video files
	 */
	private void mergeVideoIntoOneFile() {
		previewCameraLayout.removeAllViews();
		File dir = new File(folderPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		videofile = new File(dir, fileName + ".mp4");
		VideoMerger videoMerger = new VideoMerger(CuxtomCamActivity.this,
				videoFiles, videofile);
		videoMerger.setVideoMergeListener(new OnVideoListener() {

			@Override
			public void onVideoMerged() {
				Intent intent = new Intent();
				intent.putExtra(CuxtomIntent.FILE_PATH, videofile.getPath());
				intent.putExtra(CuxtomIntent.FILE_TYPE, FILE_TYPE.VIDEO);
				setResult(RESULT_OK, intent);
				/*
				 * initiate media scan and put the new things into the path
				 * array to make the scanner aware of the location and the files
				 * you want to see
				 */MediaScannerConnection.scanFile(getApplicationContext(),
						new String[] { videofile.getPath() }, null,
						CuxtomCamActivity.this);
			}

			@Override
			public void onVideoMergeFailed(Exception e) {
				// TODO Auto-generated method stub

			}
		});
		videoMerger.execute();
	}

	private void releaseMediaRecorder() {
		if (recorder != null) {
			try {
				recorder.stop();

			} catch (Exception e) {
				Log.e("error stopping", e.getMessage());
			}
			recorder.reset(); // clear recorder configuration
			recorder.release(); // release the recorder object
			recorder = null;
		}

		if (mCamera != null)
			mCamera.stopPreview();
		mCamera = null;
		mPreview.surfaceDestroyed(null);

	}

	@Override
	public synchronized void onCameraInit() {
		Log.e(TAG, "onCameraInit");
		if (cameraMode == CAMERA_MODE.VIDEO_MODE) {
			Log.e(TAG, "As VIDEO_MODE");
			new Thread(new Runnable() {

				@Override
				public void run() {
					Log.e(TAG, "Running runOnUiThread");
					mCamera.stopPreview();
					mCamera.unlock();
					initVideoRecorder();
					Log.e(TAG, "Start recorder");
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							initVideoRecordingUI("00:00");
							Log.e(TAG, "Recorder Started");
						}
					});

				}
			}).start();
			;
		}

	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
		tv_recordingDuration.removeCallbacks(recordingTimer);
		// previewCameraLayout.removeAllViewsInLayout();
		CuxtomCamActivity.this.finish();
		Log.e(TAG, "Activity Ended");
	}

}

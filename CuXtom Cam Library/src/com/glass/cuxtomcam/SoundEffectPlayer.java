package com.glass.cuxtomcam;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

public class SoundEffectPlayer {
	private MediaPlayer mShutter;
	private MediaPlayer mCamcorder;
	private MediaPlayer mCamcorderStop;
	private MediaPlayer mError;

	public void setup(Context ctx) {
		mShutter = createSoundEffect(ctx, R.raw.camera_shutter);
		mCamcorder = createSoundEffect(ctx, R.raw.camcorder);
		mCamcorderStop = createSoundEffect(ctx, R.raw.camcorder_stop);
		mError = createSoundEffect(ctx, R.raw.error);
	}

	public void deconstruct() {
		mShutter.release();
		mShutter = null;
		mCamcorder.release();
		mCamcorder = null;
		mCamcorderStop.release();
		mCamcorderStop = null;
		mError.release();
		mError = null;
	}

	public void shutter() {
		mShutter.start();
	}

	public void camcorder() {
		mCamcorder.start();
	}

	public void camcorderStop() {
		mCamcorderStop.start();
	}

	public void error() {
		mError.start();
	}

	public boolean isPlaying() {
		return mShutter.isPlaying() || mCamcorder.isPlaying()
				|| mCamcorderStop.isPlaying() || mError.isPlaying();
	}

	public void stop() {
		if (mShutter.isPlaying()) {
			mShutter.stop();
		}
		if (mCamcorder.isPlaying()) {
			mCamcorder.stop();
		}
		if (mCamcorderStop.isPlaying()) {
			mCamcorderStop.stop();
		}
		if (mError.isPlaying()) {
			mError.stop();
		}
	}

	public void reset() {
		mShutter.reset();
		mCamcorder.reset();
		mCamcorderStop.reset();
		mError.reset();
	}

	private MediaPlayer createSoundEffect(Context ctx, int resource) {
		final MediaPlayer effect = MediaPlayer.create(ctx, resource);
		effect.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer arg0) {
				if (effect != null) {
					effect.seekTo(0);
				}
			}
		});
		return effect;
	}
}

package com.glass.cuxtomcam;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

class VideoMerger extends AsyncTask<Void, Integer, Void> {
	interface OnVideoListener {
		public void onVideoMerged();

		public void onVideoMergeFailed(Exception e);

	}

	private ArrayList<File> videoFiles;
	private File finalVideoFile;
	private Exception exception;
	private OnVideoListener mListener;
	private Dialog progressDialog;
	private Context context;
	private TextView tv_loadingMessage;
	private long finalVideoFileSize;
	// Whether the thread that is checking file size is running or not
	private boolean fileSizeCheckerThread;

	public VideoMerger(Context context, ArrayList<File> videoFiles,
			File finalVideoFile) {
		super();
		this.context = context;
		this.videoFiles = videoFiles;
		this.finalVideoFile = finalVideoFile;
	}

	public void setVideoMergeListener(OnVideoListener mListener) {
		this.mListener = mListener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog = new Dialog(context);
		progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view = LayoutInflater.from(context).inflate(
				R.layout.layout_fullscreenprogressbar, null);
		tv_loadingMessage = (TextView) view
				.findViewById(R.id.tv_loadingMessage);
		tv_loadingMessage.setText("Merging 0%");
		progressDialog.setContentView(view);
		progressDialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		tv_loadingMessage.setText("Merging " + values[0] + "%");
	}

	@Override
	protected Void doInBackground(Void... params) {
		List<Movie> movies = new ArrayList<Movie>();
		List<Track> videoTracks = new LinkedList<Track>();
		List<Track> audioTracks = new LinkedList<Track>();
		try {
			for (int i = 0; i < videoFiles.size(); i++) {
				movies.add(MovieCreator.build(videoFiles.get(i)
						.getAbsolutePath()));
			}
			for (Movie m : movies) {
				for (Track t : m.getTracks()) {
					if (t.getHandler().equals("soun")) {
						audioTracks.add(t);
					}
					if (t.getHandler().equals("vide")) {
						videoTracks.add(t);
					}
				}
			}

			Movie finalMovie = new Movie();

			if (audioTracks.size() > 0) {
				finalMovie.addTrack(new AppendTrack(audioTracks
						.toArray(new Track[audioTracks.size()])));
			}
			if (videoTracks.size() > 0) {
				finalMovie.addTrack(new AppendTrack(videoTracks
						.toArray(new Track[videoTracks.size()])));
			}

			Container out = new DefaultMp4Builder().build(finalMovie);

			FileChannel fc = new RandomAccessFile(finalVideoFile, "rw")
					.getChannel();
			for (int i = 0; i < out.getBoxes().size(); i++) {
				finalVideoFileSize += out.getBoxes().get(i).getSize();
			}
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					fileSizeCheckerThread = true;
					Log.e("VideoMerge", "Started");
					long currentFileSize = 0;
					if (finalVideoFile.exists())
						currentFileSize = finalVideoFile.length();
					while (currentFileSize < finalVideoFileSize) {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (finalVideoFile.exists()) {
							currentFileSize = finalVideoFile.length();
							Log.e("VideoMerge", "Total: " + finalVideoFileSize
									+ " current: " + currentFileSize);
							int progress = (int) (currentFileSize * 100 / finalVideoFileSize);
							publishProgress(progress);
						}
						if (currentFileSize >= finalVideoFileSize)
							break;

					}
					fileSizeCheckerThread = false;
					Log.e("VideoMerge", "Ended");

				}
			});
			t.start();

			out.writeContainer(fc);
			fc.close();
			for (int i = 0; i < videoFiles.size(); i++) {
				videoFiles.get(i).delete();
			}
			publishProgress(100);
			if (fileSizeCheckerThread)
				Thread.sleep(5000);

		} catch (Exception e) {
			exception = e;
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		progressDialog.dismiss();
		if (mListener != null) {
			if (exception != null)
				mListener.onVideoMergeFailed(exception);
			else
				mListener.onVideoMerged();
		}
	}

}

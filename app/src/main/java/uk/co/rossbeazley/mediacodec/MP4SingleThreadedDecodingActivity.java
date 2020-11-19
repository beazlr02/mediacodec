package uk.co.rossbeazley.mediacodec;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MP4SingleThreadedDecodingActivity extends Activity implements SurfaceHolder.Callback {

	private MediaExtractorAndDecoderThread mPlayer = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SurfaceView sv = new SurfaceView(this);
		sv.getHolder().addCallback(this);
		setContentView(sv);
	}

	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mPlayer == null) {
			try {
				mPlayer = new MediaExtractorAndDecoderThread(holder.getSurface(), getAssets().openFd("sample.mp4"));
				mPlayer.setUncaughtExceptionHandler((e,t) -> {

					MP4SingleThreadedDecodingActivity.this.finish();
				});
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			mPlayer.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mPlayer != null) {
			mPlayer.interrupt();
		}
	}

	private class MediaExtractorAndDecoderThread extends Thread {
		private MediaExtractor extractor;
		private MediaCodec decoder;
		private Surface surface;
		private final AssetFileDescriptor assetFileDescriptor;
		private boolean allReleased;

		public MediaExtractorAndDecoderThread(Surface surface, AssetFileDescriptor assetFileDescriptor) {
			this.surface = surface;
			this.assetFileDescriptor = assetFileDescriptor;
		}

		@Override
		public void run() {
			extractor = new MediaExtractor();
			try {
				extractor.setDataSource(assetFileDescriptor);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			log("extractor has tracks " + extractor.getTrackCount());


			// expecting just 1 video track
			for (int i = (extractor.getTrackCount()-1); i > -1 ; i--) {

				MediaFormat format = extractor.getTrackFormat(i); // init segment in dash i think
				log("track " + i + " format " + format);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("video/")) {
					extractor.selectTrack(i);
					try {
						decoder = MediaCodec.createDecoderByType(mime);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					decoder.configure(format, surface, null, 0);
					break;
				}
			}

			if (decoder == null) {
				Log.e("DecodeActivity", "Can't find video info!");
				return;
			}

			decoder.start();


			boolean moreFramesToDisplay = true;
			long startTime = System.nanoTime();

				while (moreFramesToDisplay) {


					int inIndex = decoder.dequeueInputBuffer(10000);
					if (inIndex >= 0) {
						ByteBuffer buffer = decoder.getInputBuffer(inIndex);
						int sampleSize = extractor.readSampleData(buffer, 0);

						if (sampleSize < 0) {
							//its the end of media
							log("InputBuffer BUFFER_FLAG_END_OF_STREAM");
							decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
							moreFramesToDisplay = false;
						} else {
							final long mediaPresentationTime = extractor.getSampleTime();
							decoder.queueInputBuffer(inIndex, 0, sampleSize, mediaPresentationTime, 0);
							extractor.advance();
						}
					} else {

					}


					BufferInfo info = new BufferInfo();
					log("dequeueOutputBuffer");
					int outIndex = decoder.dequeueOutputBuffer(info, 10000);
					switch (outIndex) {
						case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
							log("INFO_OUTPUT_BUFFERS_CHANGED");
							break;
						case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
							log("New format " + decoder.getOutputFormat());
							break;
						case MediaCodec.INFO_TRY_AGAIN_LATER:
							log("dequeueOutputBuffer timed out!");
							break;
						default:

							while ( (info.presentationTimeUs*1000)  > (System.nanoTime() - startTime)) {
								try {
									sleep(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
									break;
								}
							}

							log("OutputBuffer info flags " + Integer.toBinaryString(info.flags));
							decoder.releaseOutputBuffer(outIndex, true);
							break;
					}



					// All decoded frames have been rendered, we can stop playing now
					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						log("OutputBuffer BUFFER_FLAG_END_OF_STREAM");
						decoder.stop();
						decoder.release();
						extractor.release();

						allReleased = true;

						//throw new RuntimeException("ALL DONE");
					} else {
						log("carry on");
					}
				}

				if(allReleased) {
					log("done");
				}
				else {
					log("no EOS found in buffer info");
					decoder.stop();
					decoder.release();
					extractor.release();

					allReleased = true;
				}

				MP4SingleThreadedDecodingActivity.this.finish();
		}


	}

	private static void log(String message) {
		Log.d("DecodeActivity", message);
	}
}
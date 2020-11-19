package uk.co.rossbeazley.mediacodec

import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

class MP4MultiThreadedDecodingActivity : Activity(), SurfaceHolder.Callback {

    lateinit var decoder : MediaCodec
    var NOT_DONE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //but only when surface created
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)

    }

    private fun readInitialisationData(assetFileDescriptor: AssetFileDescriptor): MediaFormat {


        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(assetFileDescriptor)

            for (i in extractor.getTrackCount() - 1 downTo -1 + 1) {
                val format: MediaFormat = extractor.getTrackFormat(i)
                logInput("track $i format $format")
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    extractor.release()
                    return format
                }
            }

        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return MediaFormat()
    }


    private fun configureMediaCodec(format: MediaFormat, surface: Surface): MediaCodec {
        try {
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME) ?: "")

            decoder.configure(format, surface, null, 0)
            return decoder
        } catch (e : IOException) {
            throw RuntimeException(e)
        }

    }

    private fun feedCodecWithMedia(assetFileDescriptor: AssetFileDescriptor, decoder: MediaCodec) {

       Thread() {

           val extractor = MediaExtractor()
           try {
               extractor.setDataSource(assetFileDescriptor)

           } catch (e: IOException) {
               throw RuntimeException(e)
           }

           areGotosBad@ for (i in extractor.getTrackCount() - 1 downTo -1 + 1) {
               val format: MediaFormat = extractor.getTrackFormat(i)
               val mime = format.getString(MediaFormat.KEY_MIME)
               if (mime.startsWith("video/")) {
                   extractor.selectTrack(i)
                   break@areGotosBad
               }
           }

           while (NOT_DONE) {
               logInput("Get some data")
               val inIndex = decoder.dequeueInputBuffer(10000)
               if (inIndex >= 0) {
                   logInput("Got an input buffer to fill")
                   val buffer = decoder.getInputBuffer(inIndex)
                   val sampleSize = extractor.readSampleData(buffer!!, 0)
                   if (sampleSize < 0) {
                       //its the end of media
                       logInput("Got EOF")
                       decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                       NOT_DONE = false
                   } else {
                       val mediaPresentationTime = extractor.sampleTime
                       logInput("Enqueing at $mediaPresentationTime ${sampleSize}bytes")
                       decoder.queueInputBuffer(inIndex, 0, sampleSize, mediaPresentationTime, 0)
                       extractor.advance()
                   }
               } else {
                   logInput("No buffers")
                   //NOT_DONE = false //is this an exit or a try again?
               }
           }

           extractor.release()

       }.start()

    }

    private fun renderToScreen(decoder: MediaCodec, surface: Surface) {

        Thread() {
            decoder.setOutputSurface(surface)
            val startTime = System.nanoTime()

            val info = MediaCodec.BufferInfo()


            while (NOT_DONE) {

                logOutput("dequeueOutputBuffer")
                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> logOutput("INFO_OUTPUT_BUFFERS_CHANGED")
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> logOutput("New format " + decoder.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> logOutput("dequeueOutputBuffer timed out!")
                    else -> {
                        logOutput("Got a buffer at ${info.presentationTimeUs} and current presentation time ${System.nanoTime() - startTime}")
                        while (info.presentationTimeUs * 1000 > System.nanoTime() - startTime) {
                            try {
                                Thread.sleep(10)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                                break
                            }
                        }
                        logOutput("OutputBuffer info flags " + Integer.toBinaryString(info.flags))
                        decoder.releaseOutputBuffer(outIndex, true)
                    }
                }


                // All decoded frames have been rendered, we can stop playing now


                // All decoded frames have been rendered, we can stop playing now
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    logOutput("OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    decoder.stop()
                    decoder.release()
                } else {
                    logOutput("carry on")
                }
            }
        }.start()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {


        val assetFileDescriptor = getAssets().openFd("sample.mp4")
        val mediaFormat = readInitialisationData(assetFileDescriptor)
        assetFileDescriptor.close()

        decoder = configureMediaCodec(mediaFormat, holder.surface)

        decoder.start()

        //thread 1
        feedCodecWithMedia(getAssets().openFd("sample.mp4"), decoder)
        //thread 2
        renderToScreen(decoder, holder.surface)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {}


    private fun logInput(message: String) {
        Log.d("DecodeActivityInput", message)
    }

    private fun logOutput(message: String) {
        Log.d("DecodeActivityOutput", message)
    }

}
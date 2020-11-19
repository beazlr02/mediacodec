package uk.co.rossbeazley.mediacodec

import android.app.Activity
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class MultiThreadedExtractorDecodingActivity : Activity(), SurfaceHolder.Callback {

    lateinit var extracter: VideoM4SExtractor
    private lateinit var initExtractor: InitM4SExtractor
    var NOT_DONE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)
        givenASegmentIsLoadedIntoMemory()
    }

    fun givenASegmentIsLoadedIntoMemory() {
        extracter =     VideoM4SExtractor(bytesFor("p087hv48/seg1.m4s"))
        initExtractor = InitM4SExtractor(bytesFor("p087hv48/init.dash"))
    }

    private fun bytesFor(assetFileName: String): ByteArray {
        val assetFileDescriptor = getAssets().openFd(assetFileName)
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        return bytes
    }

    private fun startPlayback(toSurface: Surface) {
        logMain("Start Playback")
        val decoder = cobbleTogetherACodec(toSurface)
        //thread 1
        feedCodecWithMedia(extracter, decoder)
        //thread 2
        renderToScreen(decoder, toSurface)
    }

    private fun cobbleTogetherACodec(surface : Surface): MediaCodec {

        logInput("cobbleTogetherACodec, should really parse something to work this out -_-")

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 192, 108)
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level21)
        format.setFloat(MediaFormat.KEY_FRAME_RATE, 25.0f)
        format.setInteger(MediaFormat.KEY_ROTATION, 0)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 540)
        format.setInteger(MediaFormat.KEY_HEIGHT, 108)
        format.setInteger(MediaFormat.KEY_WIDTH, 192)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 3916) //80
        format.setInteger(MediaFormat.KEY_PRIORITY, 0)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 960)
        format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL,1.0f)

        val decoderName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format)
        val decoder = MediaCodec.createByCodecName(decoderName)
        logInput("CREATED")

        decoder.configure(format, surface, null, 0)
        logInput("CONFIGURED")
        decoder.start()
        logInput("STARTED")

        SystemClock.sleep(1000)
        return decoder
    }


    private fun feedCodecWithMedia(extracter: VideoM4SExtractor, decoder: MediaCodec) {
       Thread {
           val frameCount = extracter.frameCount
           for(frame in 0 until frameCount) {
               feedCodec(extracter, decoder, frame)
               SystemClock.sleep(40)
           }
           logInput("all done with the frames")
           sendEOS(decoder, frameCount)
           logInput("ENDING INPUT THREAD")
       }.start()
    }

    private fun feedCodec(extracter: VideoM4SExtractor, decoder: MediaCodec, frameIdx: Int) {
        logInput("Feeding sample          ===== ${frameIdx}")
        var nalCount = 0
        var inIndex: Int = dequeueAnInputBufferIndexFromDecoder(decoder)
        val buffer = decoder.getInputBuffer(inIndex)!!
        buffer.clear()

        val microSeconds = frameIdx * (1_000_000 / 25L)

        extracter.naluForSample(frameIdx).forEach {
            logInput("Feeding    nal          ===== ${nalCount}")
            buffer.putInt(0x00)
            buffer.put(0x01)
            buffer.put(it)
            nalCount+=1
        }
        buffer.flip()

        var flags : Int = flagsFor(frameIdx = frameIdx)
        var sampleSize = buffer.limit()
        decoder.queueInputBuffer(inIndex, frameIdx, sampleSize, microSeconds, flags)
        logInput("queued buffer $inIndex with nalu ${frameIdx},${nalCount}. Size was $sampleSize and presentation time was $microSeconds and flags $flags ==================")
    }

    private fun flagsFor(frameIdx: Int) = when (frameIdx) {
            0 -> 0 or MediaCodec.BUFFER_FLAG_CODEC_CONFIG or MediaCodec.BUFFER_FLAG_KEY_FRAME
            76 ->  0 or MediaCodec.BUFFER_FLAG_KEY_FRAME
            else -> 0
        }

    private fun dequeueAnInputBufferIndexFromDecoder(decoder: MediaCodec): Int {
        var inIndex: Int = MediaCodec.INFO_TRY_AGAIN_LATER
        while (inIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

            inIndex = decoder.dequeueInputBuffer(10000)

            if (inIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                logInput("dequeueInputBuffer timed out!")
                SystemClock.sleep(500)
            }
        }

        logInput("Dequeued buffer at inIndex ${inIndex}")
        return inIndex
    }

    private fun sendEOS(decoder: MediaCodec, frameCount: Int) {
        val inIndex = decoder.dequeueInputBuffer(10000)
        if (inIndex >= 0) {
            val presentationTimeUs = frameCount * (1_000_000 / 25L)
            logInput("Got an input buffer to fill with EOS at ${presentationTimeUs}")
            decoder.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        } else {
            logInput("No buffers for EOS")
        }
    }

    private fun renderToScreen(decoder: MediaCodec, surface: Surface) {

        Thread {
            var bufferReleaseCount = 0
            val info = MediaCodec.BufferInfo()
            print("DecodeActivityOutput")
            while (NOT_DONE) {
                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> logOutput("New format INFO_OUTPUT_FORMAT_CHANGED " + decoder.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> doNothing()
                    else -> {
                        logOutput("Got a buffer at ${info.presentationTimeUs} and current presentation time ${ info.presentationTimeUs.toFloat() / 1_000_000f }")
                        logOutput("OutputBuffer info flags " + Integer.toBinaryString(info.flags))

                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0) {
                            decoder.releaseOutputBuffer(outIndex, true)
                            logOutput("RELEASED A BUFFER! ${++bufferReleaseCount}")
                        } else {
                            logOutput("OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                            decoder.stop()
                            decoder.release()
                            NOT_DONE = false
                        }
                    }
                }
            }
            logOutput("ENDING OUTPUT THREAD")
        }.start()
    }

    private fun doNothing() {
        SystemClock.sleep(10)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = startPlayback(holder.surface)//{logMain("SURFACE CHANGED")}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    private fun logMain(message: String) = Log.d("DecodeActivityMain", message)
    private fun logInput(message: String) =Log.d("DecodeActivityInput", message)
    private fun logOutput(message: String) =Log.d("DecodeActivityOutput", message)

}
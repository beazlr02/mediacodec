package uk.co.rossbeazley.mediacodec

import android.media.MediaCodec
import android.os.SystemClock

class ScreenRenderer {

    fun renderToScreen(decoder: MediaCodec) {

        Thread {
            var bufferReleaseCount = 0
            val info = MediaCodec.BufferInfo()
            print("DecodeActivityOutput")
            var NOT_DONE = true
            while (NOT_DONE) {
                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> logOutput("New format INFO_OUTPUT_FORMAT_CHANGED " + decoder.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> doNothing()
                    else -> {
                        logOutput("Got a buffer at ${info.presentationTimeUs} and current presentation time ${info.presentationTimeUs.toFloat() / 1_000_000f}")
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
}
package uk.co.rossbeazley.mediacodec

import android.media.MediaCodec
import android.os.SystemClock

class FeedsCodec
{
    fun feedCodecWithMedia(extracter: VideoM4SExtractor, decoder: MediaCodec) {
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

}
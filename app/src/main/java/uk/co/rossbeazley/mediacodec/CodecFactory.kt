package uk.co.rossbeazley.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.SystemClock
import android.view.Surface

class CodecFactory
{
    fun cobbleTogetherACodec(surface : Surface): MediaCodec {

        logInput("cobbleTogetherACodec, should really parse something to work this out -_-")

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 192, 108)
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level21)
        format.setFloat(MediaFormat.KEY_FRAME_RATE, 25.0f)
        format.setInteger(MediaFormat.KEY_ROTATION, 0)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 540)
        format.setInteger(MediaFormat.KEY_HEIGHT, 108)
        format.setInteger(MediaFormat.KEY_WIDTH, 192)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 391680)
        format.setInteger(MediaFormat.KEY_PRIORITY, 0)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 960)

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

}
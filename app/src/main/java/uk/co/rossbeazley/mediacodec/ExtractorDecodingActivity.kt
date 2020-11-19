package uk.co.rossbeazley.mediacodec

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class ExtractorDecodingActivity : Activity(), SurfaceHolder.Callback {

    lateinit var extracter: VideoM4SExtractor
    //private lateinit var initExtractor: InitM4SExtractor

    private val codecFactory = CodecFactory()

    private val feedsCodec = FeedsCodec()

    private val screenRenderer = ScreenRenderer()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        setContentView(sv)

        givenASegmentIsLoadedIntoMemory()
        startPlaybackWhenTheSurfaceIsCreated(sv)
    }


    fun givenASegmentIsLoadedIntoMemory() {
        extracter =     VideoM4SExtractor(bytesFor("p087hv48/seg1.m4s"))
        //initExtractor = InitM4SExtractor(bytesFor("p087hv48/init.dash"))
    }

    private fun bytesFor(assetFileName: String): ByteArray {
        val assetFileDescriptor = getAssets().openFd(assetFileName)
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        return bytes
    }

    private fun startPlaybackWhenTheSurfaceIsCreated(sv: SurfaceView) {
        sv.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = startPlayback(holder.surface)//{logMain("SURFACE CHANGED")}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}



    private fun startPlayback(toSurface: Surface) {
        logMain("Start Playback")
        val decoder = codecFactory.cobbleTogetherACodec(toSurface)
        //thread 1
        feedsCodec.feedCodecWithMedia(extracter, decoder)
        //thread 2
        screenRenderer.renderToScreen(decoder)
    }


}

fun logMain(message: String) = Log.d("DecodeActivityMain", message)
fun logInput(message: String) =Log.d("DecodeActivityInput", message)
fun logOutput(message: String) =Log.d("DecodeActivityOutput", message)

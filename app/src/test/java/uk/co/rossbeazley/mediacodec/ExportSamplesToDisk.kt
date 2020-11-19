package uk.co.rossbeazley.mediacodec

import org.junit.Ignore
import org.junit.Test
import java.io.FileOutputStream

class ExportSamplesToDisk {

    @Test @Ignore
    fun exportNalusStream()
    {

        var extracter: VideoM4SExtractor

        val classLoader = this::class.java.classLoader!!
        val resourceAsStream = classLoader.getResourceAsStream("p087hv48/seg1.m4s")
        val bytes: ByteArray = resourceAsStream.use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)

        val fileOutputStream = FileOutputStream("./extractor.h264")

        (0 until extracter.frameCount).forEach { nal ->
            extracter.naluForSample(nal).forEach {
                fileOutputStream.write(0x00)
                fileOutputStream.write(0x00)
                fileOutputStream.write(0x00)
                fileOutputStream.write(0x01)
                fileOutputStream.write(it)
            }
        }

        fileOutputStream.close()
    }

    @Test @Ignore
    fun exportSamples()
    {
        var extracter: VideoM4SExtractor

        val classLoader = this::class.java.classLoader!!
        val resourceAsStream = classLoader.getResourceAsStream("p087hv48/seg1.m4s")
        val bytes: ByteArray = resourceAsStream.use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)

        (0 until extracter.frameCount).forEach { frameIdx ->

            FileOutputStream("./nals/ext/extractor-${(frameIdx+1).toString().padStart(4,'0')}.h264").apply {
                extracter.naluForSample(frameIdx).forEach {
                    write(0x00)
                    write(0x00)
                    write(0x00)
                    write(0x01)
                    write(it)
                }
                close()
            }
        }

    }
}

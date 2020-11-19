package uk.co.rossbeazley.mediacodec

class TrunBox(val sampleCount: Int, boxBytes: ByteArray, val dataOffset: Int, val firstSampleFlags: Int) : Box("trun", boxBytes)
{
    val sampleRecords = mutableListOf<TrunSampleRecord>()

    /*     class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
               {
                   unsigned int(8)   version = v;
                   bit(24)  flags   =   f;
               }
               aligned(8) class TrackRunBox extends FullBox(‘trun’, version, tr_flags)
               {
                    unsigned   int(32)   sample_count;

                    // the following are optional fields
                    signed int(32) data_offset;
                    unsigned   int(32)   first_sample_flags;

                    // all fields in the following array are optional
                    {
                        unsigned   int(32)   sample_duration;
                        unsigned   int(32)   sample_size;
                        unsigned   int(32)   sample_flags
                        if (version == 0)
                        {
                            unsigned   int(32)   sample_composition_time_offset;
                        }
                        else
                        {
                            signed   int(32)      sample_composition_time_offset;
                        }
                    }[ sample_count ]
               }
     */
    companion object {
        fun from(bytes: ByteArray) : TrunBox {
            //currently ignoring full box header hence 8 (4 from box)
            val boxBytes = bytes.sliceArray(8 until bytes.size)

            var byteOffset = 0
            var numberOfBytesToConsume = 4
            val sampleCount = readBytesAsLong(numberOfBytesToConsume,boxBytes, byteOffset)
            byteOffset+=numberOfBytesToConsume

            numberOfBytesToConsume=4
            val dataOffset = readBytesAsLong(numberOfBytesToConsume,boxBytes,byteOffset)
            byteOffset+=numberOfBytesToConsume

            numberOfBytesToConsume=4
            val firstSampleFlags = readBytesAsLong(numberOfBytesToConsume,boxBytes,byteOffset) //ignored
            byteOffset+=numberOfBytesToConsume

            val trunBox = TrunBox(sampleCount.toInt(), boxBytes, dataOffset.toInt(), firstSampleFlags.toInt())

            /**
             * The following flags are defined:
             * 0x000001  data‐offset‐present.
             * 0x000004  first‐sample‐flags‐present;
             * 0x000100  sample‐duration‐present:
             * 0x000200  sample‐size‐present: each sample has its own size, otherwise the default is used.
             * 0x000400  sample‐flags‐present; each sample has its own flags, otherwise the default is used.
             * 0x000800  sample‐composition‐time‐offsets‐present
             *
             * flags=205 (data‐offset‐present, first‐sample‐flags‐present, sample‐size‐present)
             */

            for (i in 1 .. sampleCount) {
//                println("Sample record ${i}")
//                //skip sample_duration 4 bytes
//                val sampleDuration = readBytesAsLong(4,boxBytes, byteOffset)
//                byteOffset+=4
//
                //skip sample_size 4 bytes
                val sampleSize = readBytesAsLong(4, boxBytes, byteOffset)
                byteOffset+=4
//
//                //read sample_flags 4 bytes
//                val sampleFlags = readBytesAsLong(4, boxBytes, byteOffset)
//                byteOffset+=4

                trunBox.sampleRecords += TrunSampleRecord(sampleSize.toInt())
            }
            return trunBox
        }
    }
}

class TrunSampleRecord(val sampleSize: Int) {

}
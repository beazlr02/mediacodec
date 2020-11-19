package uk.co.rossbeazley.mediacodec

open class StsdBox(payload: ByteArray, val entryCount: Int, val sampleEntries: MutableMap<String, Box> = mutableMapOf()
) : Box("stsd", payload) {
    companion object {
        fun from(bytes:ByteArray) : StsdBox
        {
            /**
             * [stsd] size=12+125
                  entry_count = 1
                  [avc3] size=8+113

            aligned(8) class SampleDescriptionBox (unsigned int(32) handler_type)
                extends FullBox('stsd', version, 0){
                int i ;
                unsigned int(32) entry_count;
                for (i = 1 ; i <= entry_count ; i++){
                    SampleEntry();
                        // an instance of a class derived from SampleEntry
                }
            }
            */
            val payload = bytes.sliceArray(4 until bytes.size)
            // ignore 4 bytes its a version and flags of 0
            // read entry count as int
            val entryCount = readBytesAsLong(4,payload,4)
            val sampleEntries : MutableMap<String, Box> = mutableMapOf()
            bytesIntoBoxes(payload.sliceArray(8 until payload.size),sampleEntries)
            return StsdBox(payload, entryCount.toInt(), sampleEntries)
        }
    }
}
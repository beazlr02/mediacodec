package uk.co.rossbeazley.mediacodec


open class Avc3Box(payload: ByteArray, val dataReferenceIndex: Int, val width: Int, val height: Int, val compressorName: String, val depth: Int, val boxes: MutableMap<String, Box>) : Box("avc3", payload,  payload.size+8) {
    fun avcCBox(): Box {
        return boxes["avcC"]!!
    }

    companion object {
        fun from(bytes: ByteArray) : Avc3Box
        {
            /**
            class AVCSampleEntry() extends VisualSampleEntry (type) {
                // type is 'avc1' or 'avc3'
                AVCConfigurationBox config;
                MPEG4ExtensionDescriptorsBox (); // optional
            }

            class VisualSampleEntry(codingname) extends SampleEntry (codingname){
                unsigned int(16) pre_defined = 0;
                const unsigned int(16) reserved = 0;
                unsigned int(32)[3] pre_defined = 0;
                unsigned int(16) width;
                unsigned int(16) height;
                template unsigned int(32) horizresolution = 0x00480000; // 72 dpi
                template unsigned int(32) vertresolution = 0x00480000; // 72 dpi
                const unsigned int(32) reserved = 0;
                template unsigned int(16) frame_count = 1;
                string[32] compressorname;
                template unsigned int(16) depth = 0x0018;
                int(16) pre_defined = -1;
                // other boxes from derived specifications
                CleanApertureBox  clap;      // optional
                PixelAspectRatioBox pasp;   // optional
            }

            aligned(8) abstract class SampleEntry (unsigned int(32) format)
            extends Box(format){
                const unsigned int(8)[6] reserved = 0;
                unsigned int(16) data_reference_index;
            }

            aligned(8)
            class Box( unsigned int(32) boxtype, optional unsigned int(8)[16] extended_type)
            {
                unsigned int(32) size;
                unsigned int(32) type = boxtype;
                if (size==1) {
                    unsigned   int(64)   largesize;
                }
                else if (size==0)
                {       // box extends to end of file    }
                    if (boxtype==‘uuid’) {
                    unsigned int(8)[16] usertype = extended_type;
                }
            }

            data_reference_index is an integer that contains the index of the data reference to use to
            retrieve data associated with samples that use this sample description. Data references are
            stored in Data Reference Boxes. The index ranges from 1 to the number of data references.
             */

            //BOX
            // chop off the name bytes
            val payload = bytes.sliceArray(4 until bytes.size)

            //SampleEntry
            var byteOffset = 0
            //const unsigned int(8)[6] reserved = 0;
            byteOffset+=6
            //unsigned int(16) data_reference_index;
            val dataReferenceIndex = readBytesAsLong(2, payload, 6)
            byteOffset+=2

            //VisualSampleEntry

            //unsigned int(16) pre_defined = 0;
            byteOffset+=2
            //const unsigned int(16) reserved = 0;
            byteOffset+=2
            //unsigned int(32)[3] pre_defined = 0;
            byteOffset+=12
            //unsigned int(16) width;
            val width = readBytesAsLong(2,payload, byteOffset)
            byteOffset+=2
            //unsigned int(16) height;
            val height = readBytesAsLong(2,payload, byteOffset)
            byteOffset+=2
            //template unsigned int(32) horizresolution = 0x00480000; // 72 dpi
            byteOffset+=4
            //template unsigned int(32) vertresolution = 0x00480000; // 72 dpi
            byteOffset+=4
            //const unsigned int(32) reserved = 0;
            byteOffset+=4
            //template unsigned int(16) frame_count = 1;
            byteOffset+=2
            //string[32] compressorname;
            //first byte is the length
            val compressorNameLength = readBytesAsLong(1,payload,byteOffset)
            byteOffset+=1
            val compressorName = readBytesAsString(payload, byteOffset, compressorNameLength.toInt())
            byteOffset+=31
            //template unsigned int(16) depth = 0x0018;
            val depth = readBytesAsLong(2,payload, byteOffset)
            byteOffset+=2

            //int(16) pre_defined = -1;
            byteOffset+=2
            //// other boxes from derived specifications
            //CleanApertureBox  clap;      // optional
            //PixelAspectRatioBox pasp;   // optional

            //AVCConfigurationBox config;
            //MPEG4ExtensionDescriptorsBox (); // optional


            val remainingBytes = payload.sliceArray(byteOffset until payload.size)

            var boxes: MutableMap<String, Box> = mutableMapOf()


            bytesIntoBoxes(remainingBytes, boxes)


            return Avc3Box(payload, dataReferenceIndex.toInt(), width.toInt(), height.toInt(), compressorName, depth.toInt(), boxes)
        }
    }
}
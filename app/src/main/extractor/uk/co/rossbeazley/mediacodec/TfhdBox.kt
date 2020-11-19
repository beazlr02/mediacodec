package uk.co.rossbeazley.mediacodec

class TfhdBox(val trackID: Int, boxBytes: ByteArray) : Box("tfhd", boxBytes)
{
/*     class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
    {
        unsigned int(8)   version = v;
        bit(24)  flags   =   f;
    }
    aligned(8) class TrackFragmentHeaderBox  extends   FullBox(‘tfhd’,   0,   tf_flags)
    {
         unsigned   int(32)   track_ID;

         // all the following are optional fields
         unsigned   int(64)   base_data_offset;
         unsigned   int(32)   sample_description_index;
         unsigned   int(32)   default_sample_duration;
         unsigned   int(32)   default_sample_size;
         unsigned   int(32)   default_sample_flags
     } */
    companion object {
        fun from(bytes: ByteArray) : TfhdBox {
            //currently ignoring full box header hence 8
            val boxBytes = bytes.sliceArray(8 until bytes.size)
            val trackID =  readBitsAsLong(32,boxBytes)
            return TfhdBox(trackID.toInt(), boxBytes)
        }
    }
}
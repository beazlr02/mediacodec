package uk.co.rossbeazley.mediacodec

class TfdtBox(val mediaDecodeTime: Long, boxBytes: ByteArray) : Box("tfdt", boxBytes)
{
    /*     class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
           {
               unsigned int(8)   version = v;
               bit(24)  flags   =   f;
           }
           aligned(8) class TrackFragmentBaseMediaDecodeTimeBox    extends FullBox(‘tfdt’, version, 0)
           {
              if (version==1)
              {
                     unsigned   int(64)   baseMediaDecodeTime;
              }
              else
              { // version==0
                     unsigned   int(32)   baseMediaDecodeTime;
              }
           }
 */
    companion object {
        fun from(bytes: ByteArray) : TfdtBox {
            //currently ignoring full box header hence 8
            //ignore box name 4 bytes
            val boxBytes = bytes.sliceArray(8 until bytes.size)
            val mediaDecodeTime = readBitsAsLong(64,boxBytes)
            return TfdtBox(mediaDecodeTime, boxBytes)
        }
    }
}
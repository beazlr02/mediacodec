package uk.co.rossbeazley.mediacodec

class MfhdBox(val sequenceNumber: Int, boxByes: ByteArray) : Box("mfhd", boxByes) {


    companion object {
        fun from(bytes: ByteArray): MfhdBox {
            // (size 4 bytees  already sliced)
            // name 4 bytes
            // version 1 bytes;
            // flags 3 bytes
            // PAYLOAD
            // sequence_number 4 bytes
            val byteOffset = 8
            val boxByes = bytes.sliceArray(byteOffset until bytes.size)
            val sequenceNumber =  readBitsAsLong(32,boxByes)
            return MfhdBox(sequenceNumber.toInt(), boxByes)
        }
    }
}
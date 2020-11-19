package uk.co.rossbeazley.mediacodec

class MdatBox(boxByes: ByteArray) : Box("mdat", boxByes, boxByes.size+8) {
    companion object {
        fun from(bytes: ByteArray): MdatBox {
            //4 bytes for the box name
            return MdatBox(bytes.sliceArray(4 until bytes.size))
        }
    }
}
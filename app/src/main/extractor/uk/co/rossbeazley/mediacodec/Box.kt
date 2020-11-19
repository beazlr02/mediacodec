package uk.co.rossbeazley.mediacodec

open class Box(val name: String, val payload: ByteArray, val size: Int = payload.size+8) {
    companion object {
        fun from(bytes:ByteArray, name:String) : Box
        {
            return Box(name, bytes.sliceArray(4 until bytes.size))
        }
    }
}
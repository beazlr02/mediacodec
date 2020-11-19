package uk.co.rossbeazley.mediacodec

public fun bytesIntoBoxes(theBytes: ByteArray, boxes: MutableMap<String, Box>) {
    var theBytes1 = theBytes
    while (theBytes1.size > 0) {
        val (box, remainingBytes) = sliceOffOneBox(theBytes1)
        boxes.addBox(box)
        theBytes1 = remainingBytes
    }
}


public fun sliceOffOneBox(bytes: ByteArray): Pair<Box, ByteArray> {
    val boxSize = boxSize(bytes, 0)
    //slice off Moof worth of bytes
    val sizeHeader = 4
    val boxBytes = bytes.sliceArray(sizeHeader until boxSize.toInt())

    val box = parseBox(boxBytes)
    val remainingBytes = bytes.sliceArray(boxSize.toInt() until bytes.size)
    return Pair(box, remainingBytes)
}
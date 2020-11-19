package uk.co.rossbeazley.mediacodec

class InitM4SExtractor(val bytes: ByteArray) {


    val boxes: MutableMap<String, Box> = mutableMapOf()

    init {


        //moof, it has boxes in it
        // unsigned int(32) size;
        // unsigned int(32) type = boxtype;

        var theBytes = bytes
        while(theBytes.size > 0) {
            val (box, remainingBytes) = sliceOffOneBox(theBytes)
            boxes.addBox(box)
            theBytes = remainingBytes
        }
    }

    private fun sliceOffOneBox(bytes: ByteArray): Pair<Box, ByteArray> {
        val moofSize = boxSize(bytes, 0)
        //slice off Moof worth of bytes
        val sizeHeader = 4
        val moofBytes = bytes.sliceArray(sizeHeader until moofSize.toInt())

        val moofBox = parseBox(moofBytes)
        val remainingBytes = bytes.sliceArray(moofSize.toInt() until bytes.size)
        return Pair(moofBox, remainingBytes)
    }

    fun avcCBox(): Box {
        val moov = boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val mdia = trak.boxes["mdia"] as BoxOfBoxes
        val minf = mdia.boxes["minf"] as BoxOfBoxes
        val stbl = minf.boxes["stbl"] as BoxOfBoxes
        val stsd = stbl.boxes["stsd"] as StsdBox
        val box = stsd.sampleEntries["avc3"] as Avc3Box
        return box.avcCBox()
    }


}


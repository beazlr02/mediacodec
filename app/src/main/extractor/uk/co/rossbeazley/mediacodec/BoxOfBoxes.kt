package uk.co.rossbeazley.mediacodec

class BoxOfBoxes(name:String, boxByes: ByteArray) : Box(name, boxByes, boxByes.size+8) {

    var boxes: MutableMap<String, Box> = mutableMapOf()

    companion object {

        fun from(bytes: ByteArray, boxName: String): BoxOfBoxes {

            val moofBox = BoxOfBoxes(boxName, bytes.sliceArray(4 until bytes.size))

            var theBytes = moofBox.payload
            val boxes = moofBox.boxes

            bytesIntoBoxes(theBytes, boxes)

            return moofBox
        }


    }
}
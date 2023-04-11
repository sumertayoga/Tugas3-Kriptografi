class SBox {
     var sbox_table : List<List<Int>>
     var inv_table : List<List<Int>>

    constructor(key: String){
        val res = hexToBinary(md5(key).uppercase())

        var chunkArr = arrayOf(res,"0","0","0","0","0","0","0","0","0","0","0","0","0","0","0")
        for (i in 0..14){
            chunkArr[i+1] = (xorBinary(chunkArr[i], cyclicShift(chunkArr[i])))
        }

        for (i in 0..15){
            chunkArr[i] = binToHex(chunkArr[i])
        }

        var tempArr = Array(256) {0}
        for(i in 0..15){
            for(j in 0..15){
                tempArr[i*16 + j] = (chunkArr[i].substring((j*2), (j+1)*2)).toInt(radix = 16)
            }
        }

        var unique = tempArr.toSet().toIntArray()
        for(i in 0..255){
            if(i !in unique){
                unique = unique.plus(i)
            }
        }

        var mat = unique.toList().chunked(16)
        this.sbox_table = mat

        var hex = arrayOf("0", "1", "2", "3", "4",
            "5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e",
            "f")

        var invMat = MutableList(16){ MutableList(16) {0} }
        for(i in 0..15){
            for(j in 0..15){
                var a = (hex[j] + hex[i]).toInt(16)
                var index = mat.indexOfFirst {it.contains(a)}
                var subIndex = mat[index].indexOf(a)
                invMat[i][j] = (hex[subIndex] + hex[index]).toInt(radix = 16)
            }
        }
        this.inv_table = invMat
    }

    fun substitute(input : Int, isInverse : Boolean) : Int{
        var row = (input and 0x0f)
        var col = input shr 4
        if(isInverse){
            return this.inv_table[row][col]
        } else {
            return this.sbox_table[row][col]
        }
    }

    fun subBytes(arrayBytes: UByteArray, isInverse: Boolean) : Array<Int>{
        var cipherText = Array(0){0}
        for (byte in arrayBytes){
            cipherText = cipherText.plus(this.substitute(byte.toInt(), isInverse))
        }
        return cipherText
    }
}



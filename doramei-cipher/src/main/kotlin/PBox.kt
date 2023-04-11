import kotlin.math.min

class PBox {
    var pbox_table : Array<Int>

    constructor(pbox_table : Array<Int>){
        this.pbox_table = pbox_table
    }

    fun permute(input : Array<Int>, isInverse: Boolean): Array<Int>{
        var temp = Array(0) {0}
        for (i in 0..input.size-1){
            if(isInverse){
                var idx = this.pbox_table.indexOf(i)
                temp = temp.plus(input[idx])
            } else {
                var idx = this.pbox_table[i]
                temp = temp.plus(input[idx])
            }
        }
        return temp
    }

    fun permuteAll(input : Array<Int>, isInverse: Boolean) : Array<Int>{
        var temp = Array(0) {0}
        for (i in 0..input.size-1 step 8){
            if(i + 8 <= input.size){
                temp += this.permute(input.sliceArray(i..(min(i+8, input.size)-1)), isInverse)
            } else {
                temp += input.sliceArray(i..(min(i+8, input.size)-1))
            }
        }
        return temp
    }
}
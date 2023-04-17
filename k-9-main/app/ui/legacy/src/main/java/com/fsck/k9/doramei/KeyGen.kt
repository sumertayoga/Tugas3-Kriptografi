package com.fsck.k9.doramei

import java.math.BigInteger

fun rot_word(x: BigInteger) : BigInteger {
    return ((x shr 24) or (x shl 8)) and MASK.toBigInteger()
}

fun sub_word(x: BigInteger): BigInteger {
    return (SBOX[(x shr 28).toInt() and 0xf][(x shr 24 and BigInteger.valueOf(0xf)).toInt()].toBigInteger() shl 24) or
            (SBOX[(x shr 20).toInt() and 0xf][(x shr 16 and BigInteger.valueOf(0xf)).toInt()].toBigInteger() shl 16) or
            (SBOX[(x shr 12).toInt() and 0xf][(x shr 8 and BigInteger.valueOf(0xf)).toInt()].toBigInteger() shl 8) or
            (SBOX[(x shr 4).toInt() and 0xf][(x and BigInteger.valueOf(0xf)).toInt()].toBigInteger())
}

fun to_bytes(x: Int) : String {
    var bytes = x.toBigInteger().toByteArray()
    return bytes.joinToString(" ") { it.toInt().and(0xff).toString(16).padStart(2, '0') }
}

fun to_key(x0 : Int, x1 : Int, x2 : Int, x3 : Int) : String {
    return to_bytes(x0) + to_bytes(x1) + to_bytes(x2) + to_bytes(x3)
}

fun to_bytesArr(x: BigInteger, size:Int): ByteArray{
    var temp = x.toByteArray()
    if (temp.size >= size) {
        temp =  temp.takeLast(size).toByteArray()
    }
    return temp
}

fun to_keyByteArr(x0: BigInteger, x1: BigInteger, x2:BigInteger, x3:BigInteger) : ByteArray{
    return to_bytesArr(x0,4) + to_bytesArr(x1,4) + to_bytesArr(x2,4) + to_bytesArr(x3,4)
}


fun key_expansion(x : String) : Array<ByteArray>{
    var int_x = BigInteger(md5(x),16)

    var x0 = (int_x shr 96) and MASK.toBigInteger()
    var x1 = (int_x shr 64) and MASK.toBigInteger()
    var x2 = (int_x shr 32) and MASK.toBigInteger()
    var x3 = int_x and MASK.toBigInteger()

    var key = Array(0) { byteArrayOf() }

    for (i in 0..ROUND-1){
        x0 = sub_word(rot_word(x0)) xor RCON[i].toBigInteger()
        x1 = x1 xor x0
        x2 = x2 xor x1
        x3 = x3 xor x2
        key = key.plus(to_keyByteArr(x0, x1, x2, x3))
    }

    return key
}

fun main(){
    var a = key_expansion("sabtu")
    for (i in a){
        println(i.contentToString())
    }
}

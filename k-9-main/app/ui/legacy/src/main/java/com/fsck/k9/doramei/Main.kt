package com.fsck.k9.doramei

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.math.BigInteger
import java.util.Base64

fun pad(input: String) : ByteArray{
    var temp = input.toByteArray()
    temp = temp.filterNot { it == 13.toByte() }.toByteArray()
    var pad_len = BLOCK_SIZE - (temp.size % BLOCK_SIZE)
    var pad = ByteArray(pad_len){pad_len.toByte()}

    temp = temp.plus(pad)
    return temp
}

fun unpad(input: ByteArray) : ByteArray{
    var pad_len = input.last()
    return input.sliceArray(0..input.size-1-pad_len)

}

fun subs(arr: Array<Int>) : Array<Int>{
    var temp = Array(0){0}
    for(i in arr){
        temp = temp.plus(SBOX[i shr 4][i and 0xf])
    }
    return temp
}

fun intArrToByteArr(arr : Array<Int>) : ByteArray{
    var temp = ByteArray(0)
    for (i in arr){
        temp = temp.plus(i.toByte())
    }
    return temp
}

fun byteArrayToUByteArray(byteArray: ByteArray): UByteArray {
    return UByteArray(byteArray.size) { byteArray[it].toUByte() }
}

fun ubyteArrayToByteArray(ubyteArray: UByteArray): ByteArray {
    return ByteArray(ubyteArray.size) { ubyteArray[it].toByte() }
}

@RequiresApi(Build.VERSION_CODES.O)
fun encrypt(plaintext : String, key: String) : String {
    var sbox = SBox(key)
    var pbox = PBox(arrayOf(6, 3, 0, 4, 2, 7, 5, 1))
    var keys = key_expansion(key)
    var plainText = pad(plaintext)
    var cipherText = ByteArray(0)
    for (i in 0..plainText.size - 1 step BLOCK_SIZE) {
        cipherText += (encrypt_block(plainText.sliceArray(i..i + BLOCK_SIZE - 1), keys, sbox, pbox))
    }
    return Base64.getEncoder().encodeToString(cipherText)
}

fun encrypt_block(block: ByteArray, keys: Array<ByteArray>, sBox: SBox, pBox: PBox) : ByteArray{
    var cipherBlock = block
    for (i in 0..ROUND-1) {
        var left = cipherBlock.sliceArray(0..7)
        var right = cipherBlock.sliceArray(8..15)
        var uRight = byteArrayToUByteArray(right)

        var substituted_1 = sBox.subBytes(uRight, false)
        var permutted = pBox.permuteAll(substituted_1, false)
        var substituted_2 = (subs(permutted))

        var cipherValue = BigInteger(1, intArrToByteArr(substituted_2))
        cipherValue = cipherValue xor BigInteger(1,left)
        var cipherBlockArr = to_bytesArr(cipherValue,8)
        if(cipherBlockArr.size >= 8){
            cipherBlockArr = cipherBlockArr.takeLast(8).toByteArray()
        }
        cipherBlockArr = right + cipherBlockArr
        cipherValue = xorBigInteger(BigInteger(1, cipherBlockArr), BigInteger(1, keys[i]))
        cipherBlock = to_bytesArr(cipherValue, 16)
        if(cipherBlock.size < 16){
            for (i in 1..(16-cipherBlock.size)){
                cipherBlock = ByteArray(1){0}.plus(cipherBlock)
            }
        }
    }
    return cipherBlock
}

@RequiresApi(Build.VERSION_CODES.O)
fun decrypt(cipherTeks: String, key: String) : ByteArray{
    var cipherArr = Base64.getDecoder().decode(cipherTeks);
    var sBox = SBox(key)
    var pBox = PBox(arrayOf(6, 3, 0, 4, 2, 7, 5, 1))
    var keys = key_expansion(key)
    var plaintext = ByteArray(0)

    for(i in 0..cipherArr.size-1 step BLOCK_SIZE){
        plaintext += (decrypt_block(cipherArr.sliceArray(i..i+BLOCK_SIZE-1), keys, sBox, pBox))
    }
    return (unpad(plaintext))

}

fun decrypt_block(block: ByteArray, keys: Array<ByteArray>, sBox: SBox, pBox: PBox) : ByteArray{
    var plainBlock = block
    for(i in (ROUND-1) downTo 0){
        var plainValue = xorBigInteger(BigInteger(1, plainBlock), BigInteger(1, keys[i]))
        plainBlock = to_bytesArr(plainValue,16)
        if(plainBlock.size < 16){
            for (i in 1..(16-plainBlock.size)){
                plainBlock = ByteArray(1){0}.plus(plainBlock)
            }
        }
        var left = plainBlock.sliceArray(0..7)
        var right = plainBlock.sliceArray(8..15)
        var uLeft = byteArrayToUByteArray(left)

        var substituted_1 = sBox.subBytes(uLeft, false)
        var permutted = pBox.permuteAll(substituted_1, false)
        var substituted_2 = subs(permutted)

        plainValue = BigInteger(1, intArrToByteArr(substituted_2))
        plainValue = xorBigInteger(plainValue, BigInteger(1, right))
        plainBlock = to_bytesArr(plainValue,8)
        plainBlock = plainBlock + left
    }
    return plainBlock
}

fun main() {
//    var cipherText = encrypt("dorameibestcipherintheworld", "sabtu")
//    var plaintext =  decrypt(cipherText, "sabtu")
//    println(String(cipherText))
//    println(String(unpad(plaintext)))
}

package com.fsck.k9.doramei

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val hash = md.digest(input.toByteArray(Charsets.UTF_8))
    return hash.joinToString("") {"%02x".format(it)}
}

fun hexToBinary(hexString: String): String {
    val hexDigits = "0123456789ABCDEF"
    var binaryString = ""
    for (char in hexString) {
        val decimal = hexDigits.indexOf(char)
        val binary = decimal.toString(2)
        binaryString += binary.padStart(4, '0')
    }
    return binaryString
}

fun cyclicShift(binaryString: String) : String{
    var temp = binaryString.substring(4)
    return temp + binaryString.substring(0,4)
}

fun xorBinary(binary1 : String, binary2: String): String{
    val maxLength = maxOf(binary1.length, binary2.length)
    val paddedBinaryString1 = binary1.padStart(maxLength, '0')
    val paddedBinaryString2 = binary2.padStart(maxLength, '0')

    val int1 = BigInteger(paddedBinaryString1,2)
    val int2 = BigInteger(paddedBinaryString2,2)

    val resultInt = int1 xor int2

    val resultBinaryString = (resultInt).toString(2).padStart(maxLength, '0')

    return resultBinaryString
}

fun binToHex(binaryString: String) : String{
    val intVal = BigInteger(binaryString,2)
    return intVal.toString(16)
}

fun main() {

//    val value: Int = 123456789
//    val bytes = ByteBuffer.allocate(4).putInt(value).array().reversedArray()
//    println(bytes.contentToString())

//    var p16 = PBox(arrayOf(2,5,1,0,7,3,6,4))
//    var p = arrayOf(0x63, 0x7c, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5)
//    println(p.contentToString())
//    var cipherteks = p16.permuteAll(p, true)
//    println(cipherteks.contentToString())
//    var decrypt = p16.permuteAll(cipherteks, false)
//    println(decrypt.contentToString())

//    val sbox = SBox("sabtu")
//    var my_string = "hello world!"
//    var my_byteArray = Array(0) {0}
//    for (i in my_string){
//        my_byteArray = my_byteArray.plus(i.code)
//    }
//    var ciphertext = sbox.subBytes(my_byteArray, false)
//    println(ciphertext.contentToString())
//    var decrypt = sbox.subBytes(ciphertext, true)
//    println(decrypt.contentToString())
}

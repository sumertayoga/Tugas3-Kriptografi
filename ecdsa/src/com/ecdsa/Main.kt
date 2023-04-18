package com.ecdsa

import java.math.BigInteger
import com.ecdsa.curves.Secp256k1
import com.ecdsa.hash.Sha256
import java.security.SecureRandom
import com.ecdsa.hash.Keccak


fun main() {

    fun stringToHex(input: String): String {
        return input.toByteArray().joinToString("") { "%02x".format(it) }
    }

    val myString = "Hello, world!"
    val hexString = stringToHex(myString)
    println(hexString)


//    val nya diganti dari pesan nya ?
    val privateKeyBytes = ByteArray(32)
    SecureRandom().nextBytes(privateKeyBytes)

    // Memilih sebuah bilangan bulat random private key
//    val privateKey = BigInteger(privateKeyBytes).abs()
      val privateKey = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16)

    // menghitung kunci public dari kunci private
    val keypair = KeyGenerator.generateKey(privateKey, Secp256k1)

    println("Private key: ${keypair.privateKey}, length: ${keypair.privateKey.bitLength()}")
    println("Public key X: ${keypair.publicKey.x}, length: ${keypair.publicKey.x.bitLength()}")
    println("Public key Y: ${keypair.publicKey.y}, length: ${keypair.publicKey.y.bitLength()}")

    // sign data
    val message = "cobwrhwwwrhaa jncoba"
    val data = message.toByteArray()
    val signature = Sign.signData(keypair, data, Keccak)

    // verify
    val publicKey = keypair.publicKey
    val isValid = Sign.verifySignature(publicKey, data, signature)
    println("signature ${signature}")
    println("is valid: ${isValid}")

}

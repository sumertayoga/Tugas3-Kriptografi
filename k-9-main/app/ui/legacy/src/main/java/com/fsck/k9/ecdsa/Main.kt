package com.fsck.k9.ecdsa

import java.math.BigInteger
import com.fsck.k9.ecdsa.curves.Secp256k1
import com.fsck.k9.ecdsa.hash.Keccak
import java.security.SecureRandom



fun main() {

    // val nya diganti dari pesan nya ?
    val privateKeyBytes = ByteArray(32)
    SecureRandom().nextBytes(privateKeyBytes)

    // Memilih sebuah bilangan bulat random private key
    val privateKey = BigInteger(privateKeyBytes).abs()
    //  val privateKey = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16)

    // menghitung kunci public dari kunci private
    val keypair = KeyGenerator.generateKey(privateKey, Secp256k1)

    println("Private key: ${keypair.privateKey}, length: ${keypair.privateKey.bitLength()}")
    println("Public key X: ${keypair.publicKey.x}, length: ${keypair.publicKey.x.bitLength()}")
    println("Public key Y: ${keypair.publicKey.y}, length: ${keypair.publicKey.y.bitLength()}")

    // sign data
    val message = "hello world"
    val data = message.toByteArray()
    val signature = Sign.signData(keypair, data, Keccak)

    // verify
    val publicKey = keypair.publicKey
    val isValid = Sign.verifySignature(publicKey, data, Keccak, signature)
    println("signature r: ${signature.r}")
    println("signature s: ${signature.s}")
    println("is valid: ${isValid}")

}

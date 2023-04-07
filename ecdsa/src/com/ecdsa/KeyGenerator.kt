package com.ecdsa

import java.math.BigInteger

class KeyPair (val publicKey : Point, val privateKey: BigInteger)

object KeyGenerator {

    fun generateKey (privateKey: BigInteger, curve: Curve) : KeyPair {
        val publicKey = (curve.g * privateKey)
        return KeyPair(publicKey, privateKey)
    }

}
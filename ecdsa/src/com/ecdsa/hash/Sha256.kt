package com.ecdsa.hash

import java.security.MessageDigest

object Sha256 : Hasher {
    override fun hash(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }
}
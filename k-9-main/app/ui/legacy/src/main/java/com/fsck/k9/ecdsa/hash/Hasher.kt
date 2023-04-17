package com.fsck.k9.ecdsa.hash
interface Hasher {
    fun hash (data : ByteArray) : ByteArray
}

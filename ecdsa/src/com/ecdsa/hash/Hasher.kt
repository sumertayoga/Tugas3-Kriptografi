package com.ecdsa.hash

interface Hasher {
    fun hash (data : ByteArray) : ByteArray
}
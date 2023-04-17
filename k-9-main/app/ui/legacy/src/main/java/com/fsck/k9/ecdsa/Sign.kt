package com.fsck.k9.ecdsa

import java.math.BigInteger
import com.fsck.k9.ecdsa.hash.Hasher
import java.security.SecureRandom


class Signature (val r : BigInteger, val s : BigInteger)

object Sign {

    private fun getRandomK (n : BigInteger) : BigInteger {
        val randomValue = BigInteger(256, SecureRandom())

        if (randomValue >= n || randomValue <= BigInteger.ONE) {
            return getRandomK(n)
        }

        return randomValue
    }


    fun signData (keyPair: KeyPair, data : ByteArray, hasher : Hasher) : Signature {
        // hash message diggest
        val g = keyPair.publicKey.curve.g
        val n = keyPair.publicKey.curve.n

        // Memilih sebuah bilangan bulat random k
        val k = getRandomK(n) % n

        //  Menghitung qa = k • G = (x1,y1)
        val qa = g * k

        // val r = qa.x mod n
        val r = qa.x % n
        // jika r = 0 maka kembali ke langkah 1
        if (r == Constants.ZERO) {
            return signData(keyPair, data, hasher)
        }

        // Menghitung e = Hash(m)
        val e = BigInteger(1, hasher.hash(data))

        println("hash ${e.bitLength()}")

        // Menghitung s = k^-1 {e+privateKey • r} mod n
        val s = (k.modInverse(n)) * (e + (keyPair.privateKey * r)) % n

        if (s == Constants.ZERO) {
            signData(keyPair, data, hasher)
        }

        // tanda tangan message m adalah (r,s)
        return Signature(r, s)
    }


    fun verifySignature (publicKey : Point, data: ByteArray, hasher: Hasher, signature: Signature) : Boolean {
        if (!(signature.r.bitLength()<=256 && signature.r.bitLength()>0 && signature.s.bitLength()<=256 && signature.s.bitLength()>0)){
            return false
        }

        // Menghitung e = Hash (m)
        val e = BigInteger(1, hasher.hash(data))
        val g = publicKey.curve.g
        val n = publicKey.curve.n
        val r = signature.r
        val s = signature.s

        if (r < BigInteger.ONE || r > n - BigInteger.ONE) {
            return false
        }

        if (s < BigInteger.ONE || s > n - BigInteger.ONE) {
            return false
        }

        // Menghitung w = s-1 mod n
        val c = s.modInverse(n)

        // Menghitung u1 = ew mod n dan u2 = rw mod n
        val u1 = (e * c) % n
        val u2 = (r * c) % n
        // Menghitung u1 • G + u2 • QA = (x1,y1
        val xy = g * u1 + publicKey * u2

        // Menghitung v = x1 mod n
        val v = xy.x % n

        // Menerima tanda tangan jika dan hanya jika v = r
        println("signature v: ${v}")
        return v == r
    }
}

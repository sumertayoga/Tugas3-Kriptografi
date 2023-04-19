package com.fsck.k9.ecdsa

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fsck.k9.ecdsa.hash.Hasher
import com.fsck.k9.ecdsa.hash.Keccak
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*


class Signature (val r : BigInteger, val s : BigInteger){
    @JvmName("getR1")
    fun getR() : BigInteger {
        return r;
    }

    @JvmName("getS1")
    fun getS(): BigInteger{
        return s;
    }
}

object Sign {

    private fun getRandomK (n : BigInteger) : BigInteger {
        val randomValue = BigInteger(256, SecureRandom())

        if (randomValue >= n || randomValue <= BigInteger.ONE) {
            return getRandomK(n)
        }

        return randomValue
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun signData (keyPair: KeyPair, data : ByteArray, hasher : Hasher) : String {
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

        // convert r and s to byte arrays
        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()

        // concatenate r and s byte arrays

        // concatenate r and s byte arrays
        val rString = Base64.getEncoder().encodeToString(rBytes)
        val sString = Base64.getEncoder().encodeToString(sBytes)
        // encode concatenated byte array using Base64 encoding

        // encode concatenated byte array using Base64 encoding
        val outputSignature: String = rString + "," +sString

        return outputSignature
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun verifySignature (publicKey : Point, data: ByteArray, signature: String) : Boolean {
        val signatureParts = signature.split(",".toRegex()).toTypedArray()
        val rBytes = Base64.getDecoder().decode(signatureParts[0])
        val sBytes = Base64.getDecoder().decode(signatureParts[1])


        // create BigInteger objects from byte arrays representing r and s

        // create BigInteger objects from byte arrays representing r and s
        val signature_r = BigInteger(1, rBytes)
        val signature_s = BigInteger(1, sBytes)

        if (!(signature_r.bitLength()<=256 && signature_r.bitLength()>0 && signature_s.bitLength()<=256 && signature_s.bitLength()>0)){
            return false
        }

        // Menghitung e = Hash (m)
        val e = BigInteger(1, Keccak.hash(data))
        val g = publicKey.curve.g
        val n = publicKey.curve.n
        val r = signature_r
        val s = signature_s

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

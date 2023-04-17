package com.fsck.k9.ecdsa.hash

object Keccak : Hasher{
    fun ROL64(a: Long, n: Int): Long {
        return ((a shr (64 - (n % 64))) + (a shl (n % 64))) % (1L shl 64)
    }

    fun KeccakF1600onLanes(lanes: Array<LongArray>) {
        var R: Int = 1
        for (round in 0..24) {
            var C = LongArray(5)
            var D = LongArray(5)
            for (i in 0..4) {
                C[i] = lanes[i][0] xor lanes[i][1] xor lanes[i][2] xor lanes[i][3] xor lanes[i][4]
            }
            for (i in 0..4) {
                D[i] = C[(i + 4) % 5] xor ROL64(C[(i + 1) % 5], 1)
            }
            for (i in 0..4) {
                for (j in 0..4) {
                    lanes[i][j] = lanes[i][j] xor D[i]
                }
            }
            var (i, j) = Pair(1, 0)
            var current = lanes[i][j]
            for (t in 0..23) {
                val (new_i, new_j) = Pair(j, (2 * i + 3 * j) % 5)
                val temp = lanes[new_i][new_j]
                lanes[new_i][new_j] = ROL64(current, R)
                current = temp
                i = new_i
                j = new_j
            }
            for (y in 0..4) {
                var t = LongArray(5)
                for (x in 0..4) {
                    t[x] = lanes[x][y]
                }
                for (x in 0..4) {
                    lanes[x][y] = t[x] xor ((t[(x + 1) % 5]).inv() and t[(x + 2) % 5])
                }
            }
            for (k in 0..6) {
                R = ((R shl 1) xor ((R shr 7) * 0x71)) % 256
                if (R and 2 != 0) {
                    lanes[0][0] = lanes[0][0] xor (1L shl ((1 shl k) - 1))
                }
            }
        }
    }

    fun load64(b: ByteArray): Long {
        var result = 0L
        for (i in 0..7) {
            result = result or ((b[i].toLong() and 0xFF) shl (8 * i))
        }
        return result
    }

    fun store64(a: Long): ByteArray {
        var result = ByteArray(8)
        for (i in 0..7) {
            result[i] = ((a shr (8 * i)) % 256).toByte()
        }
        return result
    }

    fun KeccakF1600(state: ByteArray) {
        var lanes = Array(5) { LongArray(5) }
        for (i in 0..4) {
            for (j in 0..4) {
                lanes[i][j] = load64(state.sliceArray((i + 5 * j) * 8..(i + 5 * j) * 8 + 7))
            }
        }
        KeccakF1600onLanes(lanes)
        for (i in 0..4) {
            for (j in 0..4) {
                var temp = store64(lanes[i][j])
                for (k in 0..7) {
                    state[(i + 5 * j) * 8 + k] = temp[k]
                }
            }
        }
    }

    fun Keccak(rate: Int, capacity: Int, inputBytes: ByteArray, delimitedSuffix: Byte, outputByteLen: Int): ByteArray {
        var state = ByteArray(200)
        for (i in 0..199) {
            state[i] = 0
        }
        var rateInBytes = rate / 8
        var blockSize = 0
        if ((rate + capacity != 1600) || ((rate % 8) != 0)) {
            return ByteArray(0)
        }
        var inputOffset = 0

        while (inputOffset < inputBytes.size) {
            blockSize = if (inputBytes.size - inputOffset >= rateInBytes) rateInBytes else inputBytes.size - inputOffset
            for (i in 0..blockSize - 1) {
                state[i] = (state[i].toInt() xor inputBytes[inputOffset + i].toInt()).toByte()
            }
            inputOffset += blockSize
            if (blockSize == rateInBytes) {
                KeccakF1600(state)
                blockSize = 0
            }
        }

        state[blockSize] = (state[blockSize].toInt() xor delimitedSuffix.toInt()).toByte()
        if ((delimitedSuffix.toInt() and 0x80) != 0 && (blockSize == (rateInBytes - 1))) {
            KeccakF1600(state)
        }
        state[rateInBytes - 1] = (state[rateInBytes - 1].toInt() xor 0x80).toByte()
        KeccakF1600(state)

        var result = ByteArray(0)
        var outputByteLenCopy = outputByteLen

        while (outputByteLenCopy > 0) {
            blockSize = if (outputByteLenCopy > rateInBytes) rateInBytes else outputByteLenCopy
            result = ByteArray(outputByteLenCopy)
            for (i in 0..blockSize - 1) {
                result[i] = state[i]
            }
            outputByteLenCopy -= blockSize
            if (outputByteLenCopy > 0) {
                KeccakF1600(state)
            }
        }

        return result
    }

    fun SHAKE128(inputBytes: ByteArray, outputByteLen: Int): ByteArray {
        return Keccak(1344, 256, inputBytes, 0x1F, outputByteLen)
    }

    fun SHAKE256(inputBytes: ByteArray, outputByteLen: Int): ByteArray {
        return Keccak(1088, 512, inputBytes, 0x1F, outputByteLen)
    }

    fun SHA3_224(inputBytes: ByteArray): ByteArray {
        return Keccak(1152, 448, inputBytes, 0x06, 28)
    }

    fun SHA3_256(inputBytes: ByteArray): ByteArray {
        return Keccak(1088, 512, inputBytes, 0x06, 32)
    }

    fun SHA3_384(inputBytes: ByteArray): ByteArray {
        return Keccak(832, 768, inputBytes, 0x06, 48)
    }

    fun SHA3_512(inputBytes: ByteArray): ByteArray {
        return Keccak(576, 1024, inputBytes, 0x06, 64)
    }

    override fun hash(data: ByteArray): ByteArray {
        return SHA3_256(data)
    }
}

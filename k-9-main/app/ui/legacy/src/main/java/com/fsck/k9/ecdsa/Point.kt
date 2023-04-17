package com.fsck.k9.ecdsa

import java.math.BigInteger

class Point (val x : BigInteger, val y : BigInteger, val curve: Curve) {

    operator fun plus (other: Point) : Point {
        return curve.add(this, other)
    }

    operator fun times(n: BigInteger): Point {
        return curve.multiply(this, n)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Point) {
            return (x == other.x && y == other.y)
        }

        return false
    }

    override fun hashCode(): Int {
        return x.hashCode() + y.hashCode()
    }
}

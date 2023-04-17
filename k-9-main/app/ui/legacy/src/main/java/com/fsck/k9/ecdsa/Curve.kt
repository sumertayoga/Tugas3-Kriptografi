package com.fsck.k9.ecdsa


import java.math.BigInteger

abstract class Curve {
    abstract val p : BigInteger
    abstract val n : BigInteger
    abstract val a : BigInteger
    abstract val b : BigInteger
    abstract val x : BigInteger
    abstract val y : BigInteger
    val g : Point
        get() = Point(x, y, this)


    val identity : Point
        get() = PointMath.identity(g)


    fun add (p1 : Point, p2: Point) : Point {
        if (p1.x == p) {
            return p2
        } else if (p2.x == p) {
            return p1
        }

        if (p1.x == p2.x) {
            if (p1.y == p2.y) {
                return PointMath.double(p1)
            }
            return PointMath.identity(p1)
        }

        val m = PointMath.divide(p1.y + p - p2.y, p1.x + p - p2.x, p)
        return PointMath.dot(p1, p2, m, this)
    }


    fun multiply (g : Point, n : BigInteger) : Point {
        var r = identity
        var q = g
        var m = n

        while (m != Constants.ZERO) {


            if (m and Constants.ONE != 0.toBigInteger()) {
                r = add(r, q)
            }

            m = m shr 1

            if (m != 0.toBigInteger()) {
                q = PointMath.double(q)
            }

        }

        return r
    }

    operator fun plus (point : Point) : Point {
        return add(g, point)
    }
    operator fun times(n : BigInteger) : Point {
        return multiply(g, n)
    }
}

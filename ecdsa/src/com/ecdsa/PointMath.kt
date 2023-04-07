package com.ecdsa


import java.math.BigInteger

object PointMath {

    fun multiply (a : BigInteger, b : BigInteger, prime: BigInteger) : BigInteger {
        return (a * b) % prime
    }

    fun divide (num : BigInteger, dom : BigInteger, prime: BigInteger) : BigInteger {
        val inverseDen = dom.modInverse(prime)
        return multiply(num % prime, inverseDen, prime)
    }

    fun tangent (point: Point, curve: Curve) : BigInteger {
        return divide(point.x * point.x* Constants.THREE+curve.a, point.y * Constants.TWO, curve.p)
    }

    fun identity (point: Point) : Point {
        return Point(point.curve.p, Constants.ZERO, point.curve)
    }

    fun dot (p1: Point, p2: Point, m : BigInteger, curve: Curve) : Point {
        val v = (p1.y + curve.p - (m * p1.x) % curve.p) % curve.p
        val x = (m*m + curve.p - p1.x + curve.p - p2.x) % curve.p
        val y = (curve.p - (m*x) % curve.p + curve.p - v) % curve.p
        return Point(x, y, curve)
    }

    fun double (point: Point) : Point {
        if (point.x == point.curve.p) {
            return point
        }

        return dot(point, point, tangent(point, point.curve), point.curve)
    }
}

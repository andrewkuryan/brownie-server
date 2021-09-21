package com.gitlab.andrewkuryan.brownie.logic

import org.apache.commons.codec.digest.DigestUtils
import java.math.BigInteger
import java.security.SecureRandom

class SrpGenerator(private val N: BigInteger, private val NBitLen: Int, private val g: BigInteger) {

    private val k: BigInteger

    init {
        val NHex = N.toString(16)
        val gHex = g.toString(16)
        val hashIn = (if ((NHex.length and 1) == 0) NHex else "0$NHex") + nZero(NHex.length - gHex.length) + gHex
        val kTmp = BigInteger(DigestUtils.sha3_512Hex(hashIn), 16)
        k = if (kTmp < N) kTmp else kTmp % N
    }

    fun computeKHexB(A: BigInteger, verifier: BigInteger): Pair<String, BigInteger> {
        val b = BigInteger(2048, SecureRandom())
        val B = k * verifier + g.modPow(b, N)
        val u = computeU(A, B)
        val S = (A * verifier.modPow(u, N)).modPow(b, N)
        val KHex = DigestUtils.sha3_512Hex(S.toString(16))
        return KHex to B
    }

    fun computeMHex(username: String, saltHex: String, A: String, B: String, KHex: String): String {
        val NHex = BigInteger(DigestUtils.sha3_512Hex(N.toString(16)), 16)
        val gHex = BigInteger(DigestUtils.sha3_512Hex(g.toString(16)), 16)
        return DigestUtils.sha3_512Hex(
                (NHex xor gHex).toString(16) +
                        DigestUtils.sha3_512Hex(username) +
                        saltHex + A + B + KHex
        )
    }

    fun computeRHex(A: String, MHex: String, KHex: String): String {
        return DigestUtils.sha3_512Hex(A + MHex + KHex)
    }

    fun computeU(A: BigInteger, B: BigInteger): BigInteger {
        val aHex = A.toString(16)
        val bHex = B.toString(16)
        val nLen = 2 * ((NBitLen + 7) shr 3)
        val hashIn = nZero(nLen - aHex.length) + aHex + nZero(nLen - bHex.length) + bHex
        val uTmp = BigInteger(DigestUtils.sha3_512Hex(hashIn), 16)
        return if (uTmp < N) uTmp else uTmp % (N - BigInteger.ONE)
    }

    private fun nZero(n: Int): String {
        if (n < 1) {
            return ""
        }
        val t = nZero(n shr 1)
        return if ((n and 1) == 0) {
            t + t
        } else {
            t + t + "0"
        }
    }
}
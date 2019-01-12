//package bip32
//
///*
// * Copyright 2013 bits of proof zrt.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//
//import org.bouncycastle.util.encoders.Hex
//
//import java.io.UnsupportedEncodingException
//import java.math.BigInteger
//import java.util.Arrays
//
//
//object ByteUtils {
//    private val b58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
//    private val r58 = IntArray(256)
//
//    init {
//        for (i in 0..255) {
//            r58[i] = -1
//        }
//        for (i in b58.indices) {
//            r58[b58[i].toInt()] = i
//        }
//    }
//
//    fun toBase58(b: ByteArray): String {
//        if (b.size == 0) {
//            return ""
//        }
//
//        var lz = 0
//        while (lz < b.size && b[lz].toInt() == 0) {
//            ++lz
//        }
//
//        val s = StringBuffer()
//        var n = BigInteger(1, b)
//        while (n.compareTo(BigInteger.ZERO) > 0) {
//            val r = n.divideAndRemainder(BigInteger.valueOf(58))
//            n = r[0]
//            val digit = b58[r[1].toInt()]
//            s.append(digit)
//        }
//        while (lz > 0) {
//            --lz
//            s.append("1")
//        }
//        return s.reverse().toString()
//    }
//
//    fun toBase58WithChecksum(b: ByteArray): String {
//        val cs = Hash.hash(b)
//        val extended = ByteArray(b.size + 4)
//        System.arraycopy(b, 0, extended, 0, b.size)
//        System.arraycopy(cs, 0, extended, b.size, 4)
//        return toBase58(extended)
//    }
//
////    @Throws(ValidationException::class)
////    fun fromBase58WithChecksum(s: String): ByteArray {
////        val b = fromBase58(s)
////        if (b.size < 4) {
////            throw ValidationException("Too short for checksum $s")
////        }
////        val cs = ByteArray(4)
////        System.arraycopy(b, b.size - 4, cs, 0, 4)
////        val data = ByteArray(b.size - 4)
////        System.arraycopy(b, 0, data, 0, b.size - 4)
////        val h = ByteArray(4)
////        System.arraycopy(Hash.hash(data), 0, h, 0, 4)
////        if (Arrays.equals(cs, h)) {
////            return data
////        }
////        throw ValidationException("Checksum mismatch $s")
////    }
//
//    @Throws(ValidationException::class)
//    fun fromBase58(s: String): ByteArray {
//        try {
//            var leading = true
//            var lz = 0
//            var b = BigInteger.ZERO
//            for (c in s.toCharArray()) {
//                if (leading && c == '1') {
//                    ++lz
//                } else {
//                    leading = false
//                    b = b.multiply(BigInteger.valueOf(58))
//                    b = b.add(BigInteger.valueOf(r58[c.toInt()].toLong()))
//                }
//            }
//            var encoded = b.toByteArray()
//            if (encoded[0].toInt() == 0) {
//                if (lz > 0) {
//                    --lz
//                } else {
//                    val e = ByteArray(encoded.size - 1)
//                    System.arraycopy(encoded, 1, e, 0, e.size)
//                    encoded = e
//                }
//            }
//            val result = ByteArray(encoded.size + lz)
//            System.arraycopy(encoded, 0, result, lz, encoded.size)
//
//            return result
//        } catch (e: ArrayIndexOutOfBoundsException) {
//            throw ValidationException("Invalid character in address")
//        } catch (e: Exception) {
//            throw ValidationException(e)
//        }
//
//    }
//
////    fun reverse(data: ByteArray): ByteArray {
////        var i = 0
////        var j = data.size - 1
////        while (i < data.size / 2) {
////            data[i] = data[i] xor data[j]
////            data[j] = data[j] xor data[i]
////            data[i] = data[i] xor data[j]
////            i++
////            j--
////        }
////        return data
////    }
//
////    fun toHex(data: ByteArray): String? {
////        try {
////            return String(Hex.encode(data), "US-ASCII")
////        } catch (e: UnsupportedEncodingException) {
////        }
////
////        return null
////    }
////
////    fun fromHex(hex: String): ByteArray {
////        return Hex.decode(hex)
////    }
////
////    fun isLessThanUnsigned(n1: Long, n2: Long): Boolean {
////        return (n1 < n2) xor (n1 < 0 != n2 < 0)
////    }
//}

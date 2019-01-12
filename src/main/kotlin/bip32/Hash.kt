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
//import org.bouncycastle.crypto.digests.RIPEMD160Digest
//
//import java.math.BigInteger
//import java.security.MessageDigest
//import java.security.NoSuchAlgorithmException
//
//
//class Hash {
//    private val bytes: ByteArray
//
//    constructor(hash: ByteArray) {
//        if (hash.size != 32) {
//            throw IllegalArgumentException("Digest length must be 32 bytes for Hash")
//        }
//        this.bytes = ByteArray(32)
//        System.arraycopy(hash, 0, this.bytes, 0, 32)
//    }
//
//    constructor(hex: String) {
//        if (hex.length != 64) {
//            throw IllegalArgumentException("Digest length must be 64 hex characters for Hash")
//        }
//
//        this.bytes = ByteUtils.reverse(ByteUtils.fromHex(hex))
//    }
//
//    fun toByteArray(): ByteArray {
//        val copy = ByteArray(bytes.size)
//        System.arraycopy(bytes, 0, copy, 0, bytes.size)
//        return copy
//    }
//
//    fun toBigInteger(): BigInteger {
//        val hashAsNumber = ByteArray(32)
//        System.arraycopy(bytes, 0, hashAsNumber, 0, 32)
//        ByteUtils.reverse(hashAsNumber)
//        return BigInteger(1, hashAsNumber)
//    }
//
//    override fun toString(): String {
//        return ByteUtils.toHex(ByteUtils.reverse(toByteArray()))!!
//    }
//
//    companion object {
//        val ZERO_HASH = Hash(ByteArray(32))
//        val ZERO_HASH_STRING = Hash(ByteArray(32)).toString()
//
//        fun sha256(data: ByteArray): ByteArray {
//            try {
//                val a = MessageDigest.getInstance("SHA-256")
//                return a.digest(data)
//            } catch (e: NoSuchAlgorithmException) {
//                throw RuntimeException(e)
//            }
//
//        }
//
//        fun keyHash(key: ByteArray): ByteArray {
//            val ph = ByteArray(20)
//            try {
//                val sha256 = MessageDigest.getInstance("SHA-256").digest(key)
//                val digest = RIPEMD160Digest()
//                digest.update(sha256, 0, sha256.size)
//                digest.doFinal(ph, 0)
//            } catch (e: NoSuchAlgorithmException) {
//                throw RuntimeException(e)
//            }
//
//            return ph
//        }
//
//        @JvmOverloads
//        fun hash(data: ByteArray, offset: Int = 0, len: Int = data.size): ByteArray {
//            try {
//                val a = MessageDigest.getInstance("SHA-256")
//                a.update(data, offset, len)
//                return a.digest(a.digest())
//            } catch (e: NoSuchAlgorithmException) {
//                throw RuntimeException(e)
//            }
//
//        }
//    }
//}

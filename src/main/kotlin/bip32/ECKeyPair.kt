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
//import org.bouncycastle.asn1.ASN1InputStream
//import org.bouncycastle.asn1.DERInteger
//import org.bouncycastle.asn1.DERSequenceGenerator
//import org.bouncycastle.asn1.DLSequence
//import org.bouncycastle.asn1.sec.SECNamedCurves
//import org.bouncycastle.asn1.x9.X9ECParameters
//import org.bouncycastle.crypto.AsymmetricCipherKeyPair
//import org.bouncycastle.crypto.generators.ECKeyPairGenerator
//import org.bouncycastle.crypto.params.ECDomainParameters
//import org.bouncycastle.crypto.params.ECKeyGenerationParameters
//import org.bouncycastle.crypto.params.ECPrivateKeyParameters
//import org.bouncycastle.crypto.params.ECPublicKeyParameters
//import org.bouncycastle.crypto.signers.ECDSASigner
//import org.bouncycastle.math.ec.ECPoint
//import org.bouncycastle.util.Arrays
//
//import java.io.ByteArrayOutputStream
//import java.io.IOException
//import java.math.BigInteger
//import java.security.SecureRandom
//
//
//class ECKeyPair : Key {
//
//    private var priv: BigInteger? = null
//    private var pub: ByteArray? = null
//    override var isCompressed: Boolean = false
//        private set(value: Boolean) {
//            super.isCompressed = value
//        }
//
//    override val private: ByteArray
//        get() {
//            var p = priv!!.toByteArray()
//
//            if (p.size != 32) {
//                val tmp = ByteArray(32)
//                System.arraycopy(p, Math.max(0, p.size - 32), tmp, Math.max(0, 32 - p.size), Math.min(32, p.size))
//                p = tmp
//            }
//
//            return p
//        }
//
//    override var public: ByteArray
//        get() = Arrays.clone(pub)
//        @Throws(ValidationException::class)
//        set(pub) = throw ValidationException("Can not set public key if private is present")
//
//    override val address: ByteArray
//        get() = Hash.keyHash(pub)
//
//    private constructor() {}
//
//    @Throws(CloneNotSupportedException::class)
//    override fun clone(): ECKeyPair {
//        val c = super.clone() as ECKeyPair
//        c.priv = BigInteger(c.priv!!.toByteArray())
//        c.pub = Arrays.clone(pub)
//        c.isCompressed = isCompressed
//        return c
//    }
//
//    @Throws(ValidationException::class)
//    constructor(p: ByteArray, compressed: Boolean) {
//        if (p.size != 32) {
//            throw ValidationException("Invalid private key")
//        }
//        this.priv = BigInteger(1, p).mod(curve.n)
//        this.isCompressed = compressed
//        if (compressed) {
//            val q = curve.g.multiply(priv)
//            pub = ECPoint.Fp(domain.curve, q.x, q.y, true).encoded
//        } else {
//            pub = curve.g.multiply(priv).encoded
//        }
//    }
//
//    constructor(priv: BigInteger, compressed: Boolean) {
//        this.priv = priv
//        this.isCompressed = compressed
//        if (compressed) {
//            val q = curve.g.multiply(priv)
//            pub = ECPoint.Fp(domain.curve, q.x, q.y, true).encoded
//        } else {
//            pub = curve.g.multiply(priv).encoded
//        }
//    }
//
//    @Throws(ValidationException::class)
//    override fun sign(hash: ByteArray): ByteArray? {
//        if (priv == null) {
//            throw ValidationException("Need private key to sign")
//        }
//        val signer = ECDSASigner()
//        signer.init(true, ECPrivateKeyParameters(priv, domain))
//        val signature = signer.generateSignature(hash)
//        val s = ByteArrayOutputStream()
//        try {
//            val seq = DERSequenceGenerator(s)
//            seq.addObject(DERInteger(signature[0]))
//            seq.addObject(DERInteger(signature[1]))
//            seq.close()
//            return s.toByteArray()
//        } catch (e: IOException) {
//        }
//
//        return null
//    }
//
//    override fun verify(hash: ByteArray, signature: ByteArray): Boolean {
//        return verify(hash, signature, pub)
//    }
//
//    companion object {
//        private val secureRandom = SecureRandom()
//        private val curve = SECNamedCurves.getByName("secp256k1")
//        private val domain = ECDomainParameters(curve.curve, curve.g, curve.n, curve.h)
//
//        fun createNew(compressed: Boolean): ECKeyPair {
//            val generator = ECKeyPairGenerator()
//            val keygenParams = ECKeyGenerationParameters(domain, secureRandom)
//            generator.init(keygenParams)
//            val keypair = generator.generateKeyPair()
//            val privParams = keypair.private as ECPrivateKeyParameters
//            val pubParams = keypair.public as ECPublicKeyParameters
//            val k = ECKeyPair()
//            k.priv = privParams.d
//            k.isCompressed = compressed
//            if (compressed) {
//                val q = pubParams.q
//                k.pub = ECPoint.Fp(domain.curve, q.x, q.y, true).encoded
//            } else {
//                k.pub = pubParams.q.encoded
//            }
//            return k
//        }
//
//        fun verify(hash: ByteArray, signature: ByteArray, pub: ByteArray?): Boolean {
//            val asn1 = ASN1InputStream(signature)
//            try {
//                val signer = ECDSASigner()
//                signer.init(false, ECPublicKeyParameters(curve.curve.decodePoint(pub!!), domain))
//
//                val seq = asn1.readObject() as DLSequence
//                val r = (seq.getObjectAt(0) as DERInteger).positiveValue
//                val s = (seq.getObjectAt(1) as DERInteger).positiveValue
//                return signer.verifySignature(hash, r, s)
//            } catch (e: Exception) {
//                // threat format errors as invalid signatures
//                return false
//            } finally {
//                try {
//                    asn1.close()
//                } catch (e: IOException) {
//                }
//
//            }
//        }
//
//        fun serializeWIF(key: Key): String {
//            return ByteUtils.toBase58(bytesWIF(key))
//        }
//
//        private fun bytesWIF(key: Key): ByteArray {
//            val k = key.private
//            if (key.isCompressed) {
//                val encoded = ByteArray(k.size + 6)
//                val ek = ByteArray(k.size + 2)
//                ek[0] = 0x80.toByte()
//                System.arraycopy(k, 0, ek, 1, k.size)
//                ek[k.size + 1] = 0x01
//                val hash = Hash.hash(ek)
//                System.arraycopy(ek, 0, encoded, 0, ek.size)
//                System.arraycopy(hash, 0, encoded, ek.size, 4)
//                return encoded
//            } else {
//                val encoded = ByteArray(k.size + 5)
//                val ek = ByteArray(k.size + 1)
//                ek[0] = 0x80.toByte()
//                System.arraycopy(k, 0, ek, 1, k.size)
//                val hash = Hash.hash(ek)
//                System.arraycopy(ek, 0, encoded, 0, ek.size)
//                System.arraycopy(hash, 0, encoded, ek.size, 4)
//                return encoded
//            }
//        }
//
//        @Throws(ValidationException::class)
//        fun parseWIF(serialized: String): ECKeyPair {
//            val store = ByteUtils.fromBase58(serialized)
//            return parseBytesWIF(store)
//        }
//
//        @Throws(ValidationException::class)
//        fun parseBytesWIF(store: ByteArray): ECKeyPair {
//            if (store.size == 37) {
//                checkChecksum(store)
//                val key = ByteArray(store.size - 5)
//                System.arraycopy(store, 1, key, 0, store.size - 5)
//                return ECKeyPair(key, false)
//            } else if (store.size == 38) {
//                checkChecksum(store)
//                val key = ByteArray(store.size - 6)
//                System.arraycopy(store, 1, key, 0, store.size - 6)
//                return ECKeyPair(key, true)
//            }
//            throw ValidationException("Invalid key length")
//        }
//
//        @Throws(ValidationException::class)
//        private fun checkChecksum(store: ByteArray) {
//            val checksum = ByteArray(4)
//            System.arraycopy(store, store.size - 4, checksum, 0, 4)
//            val ekey = ByteArray(store.size - 4)
//            System.arraycopy(store, 0, ekey, 0, store.size - 4)
//            val hash = Hash.hash(ekey)
//            for (i in 0..3) {
//                if (hash[i] != checksum[i]) {
//                    throw ValidationException("checksum mismatch")
//                }
//            }
//        }
//    }
//}

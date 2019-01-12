//package bip32
//
//import org.bouncycastle.asn1.sec.SECNamedCurves
//import org.bouncycastle.crypto.generators.SCrypt
//import org.bouncycastle.math.ec.ECPoint
//import org.bouncycastle.util.Arrays
//import org.web3j.crypto.ECKeyPair
//
//import javax.crypto.*
//import javax.crypto.spec.IvParameterSpec
//import javax.crypto.spec.SecretKeySpec
//import java.io.ByteArrayOutputStream
//import java.io.IOException
//import java.io.UnsupportedEncodingException
//import java.math.BigInteger
//import java.security.*
//
//fun main(args: Array<String>){
//
//}
//
///**
// * Key Generator following BIP32 https://en.bitcoin.it/wiki/BIP_0032
// */
//class ExtendedKey(
//    val master: Key,
//    private val chainCode: ByteArray,
//    val depth: Int,
//    val parent: Int,
//    val sequence: Int
//) {
//
//
////    val fingerPrint: Int
////        get() {
////            var fingerprint = 0
////            val address = master.address
////            for (i in 0..3) {
////                fingerprint = fingerprint shl 8
////                fingerprint = fingerprint or (address[i] and 0xff)
////            }
////            return fingerprint
////        }
//
////    val readOnly: ExtendedKey
////        get() = ExtendedKey(ECPublicKey(master.public, true), chainCode, depth, parent, sequence)
//
//    val isReadOnly: Boolean
//        get() = master.private == null
//
//    @Throws(ValidationException::class)
//    fun encrypt(passphrase: String, production: Boolean): ByteArray {
//        try {
//            val key = SCrypt.generate(passphrase.toByteArray(charset("UTF-8")),
//                BITCOIN_SEED, 16384, 8, 8, 32)
//            val keyspec = SecretKeySpec(key, "AES")
//            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC")
//            cipher.init(Cipher.ENCRYPT_MODE, keyspec)
//            val iv = cipher.iv
//            val c = cipher.doFinal(serialize(production).toByteArray())
//            val result = ByteArray(iv.size + c.size)
//            System.arraycopy(iv, 0, result, 0, iv.size)
//            System.arraycopy(c, 0, result, iv.size, c.size)
//            return result
//        } catch (e: Exception) {
//            throw ValidationException(e)
//        }
//
//    }
//
//    fun getChainCode(): ByteArray {
//        return Arrays.clone(chainCode)
//    }
//
////    @Throws(ValidationException::class)
////    fun getKey(sequence: Int): Key {
////        return generateKey(sequence).master
////    }
//
////    @Throws(ValidationException::class)
////    fun getChild(sequence: Int): ExtendedKey {
////        val sub = generateKey(sequence)
////        return ExtendedKey(sub.master, sub.getChainCode(), sub.depth + 1, fingerPrint, sequence)
////    }
//
////    @Throws(ValidationException::class)
////    private fun generateKey(sequence: Int): ExtendedKey {
////        try {
////            if (sequence and -0x80000000 != 0 && master.private == null) {
////                throw ValidationException("need private key for private generation")
////            }
////            val mac = Mac.getInstance("HmacSHA512", "BC")
////            val key = SecretKeySpec(chainCode, "HmacSHA512")
////            mac.init(key)
////
////            val extended: ByteArray
////            var pub = master.public
////            if (sequence and -0x80000000 == 0) {
////                extended = ByteArray(pub.size + 4)
////                System.arraycopy(pub, 0, extended, 0, pub.size)
////                extended[pub.size] = (sequence.ushr(24) and 0xff).toByte()
////                extended[pub.size + 1] = (sequence.ushr(16) and 0xff).toByte()
////                extended[pub.size + 2] = (sequence.ushr(8) and 0xff).toByte()
////                extended[pub.size + 3] = (sequence and 0xff).toByte()
////            } else {
////                val priv = master.private
////                extended = ByteArray(priv.size + 5)
////                System.arraycopy(priv, 0, extended, 1, priv.size)
////                extended[priv.size + 1] = (sequence.ushr(24) and 0xff).toByte()
////                extended[priv.size + 2] = (sequence.ushr(16) and 0xff).toByte()
////                extended[priv.size + 3] = (sequence.ushr(8) and 0xff).toByte()
////                extended[priv.size + 4] = (sequence and 0xff).toByte()
////            }
////            val lr = mac.doFinal(extended)
////            val l = Arrays.copyOfRange(lr, 0, 32)
////            val r = Arrays.copyOfRange(lr, 32, 64)
////
////            val m = BigInteger(1, l)
////            if (m.compareTo(curve.n) >= 0) {
////                throw ValidationException("This is rather unlikely, but it did just happen")
////            }
////            if (master.private != null) {
////                val k = m.add(BigInteger(1, master.private)).mod(curve.n)
////                if (k == BigInteger.ZERO) {
////                    throw ValidationException("This is rather unlikely, but it did just happen")
////                }
////                return ExtendedKey(ECKeyPair(k, true), r, depth, parent, sequence)
////            } else {
////                val q = curve.g.multiply(m).add(curve.curve.decodePoint(pub))
////                if (q.isInfinity) {
////                    throw ValidationException("This is rather unlikely, but it did just happen")
////                }
////                pub = ECPoint.Fp(curve.curve, q.x, q.y, true).encoded
////                return ExtendedKey(ECPublicKey(pub, true), r, depth, parent, sequence)
////            }
////        } catch (e: NoSuchAlgorithmException) {
////            throw ValidationException(e)
////        } catch (e: NoSuchProviderException) {
////            throw ValidationException(e)
////        } catch (e: InvalidKeyException) {
////            throw ValidationException(e)
////        }
////
////    }
//
//    fun serialize(production: Boolean): String {
//        val out = ByteArrayOutputStream()
//        try {
//            if (master.private != null) {
//                if (production) {
//                    out.write(xprv)
//                } else {
//                    out.write(tprv)
//                }
//            } else {
//                if (production) {
//                    out.write(xpub)
//                } else {
//                    out.write(tpub)
//                }
//            }
//            out.write(depth and 0xff)
//            out.write(parent.ushr(24) and 0xff)
//            out.write(parent.ushr(16) and 0xff)
//            out.write(parent.ushr(8) and 0xff)
//            out.write(parent and 0xff)
//            out.write(sequence.ushr(24) and 0xff)
//            out.write(sequence.ushr(16) and 0xff)
//            out.write(sequence.ushr(8) and 0xff)
//            out.write(sequence and 0xff)
//            out.write(chainCode)
//            if (master.private != null) {
//                out.write(0x00)
//                out.write(master.private)
//            } else {
//                out.write(master.public)
//            }
//        } catch (e: IOException) {
//        }
//
//        return ByteUtils.toBase58WithChecksum(out.toByteArray())
//    }
//
//    companion object {
//        private val rnd = SecureRandom()
//        private val curve = SECNamedCurves.getByName("secp256k1")
//
//        private val BITCOIN_SEED = "Bitcoin seed".toByteArray()
//
////        @Throws(ValidationException::class)
////        fun createFromPassphrase(passphrase: String, encrypted: ByteArray): ExtendedKey {
////            try {
////                val key = SCrypt.generate(passphrase.toByteArray(charset("UTF-8")),
////                    BITCOIN_SEED, 16384, 8, 8, 32)
////                val keyspec = SecretKeySpec(key, "AES")
////                if (encrypted.size == 32) {
////                    // asssume encrypted is seed
////                    val cipher = Cipher.getInstance("AES/ECB/NoPadding", "BC")
////                    cipher.init(Cipher.DECRYPT_MODE, keyspec)
////                    return create(cipher.doFinal(encrypted))
////                } else {
////                    // assume encrypted serialization of a key
////                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC")
////                    val iv = Arrays.copyOfRange(encrypted, 0, 16)
////                    val data = Arrays.copyOfRange(encrypted, 16, encrypted.size)
////                    cipher.init(Cipher.DECRYPT_MODE, keyspec, IvParameterSpec(iv))
////                    return parse(String(cipher.doFinal(data)))
////                }
////            } catch (e: UnsupportedEncodingException) {
////                throw ValidationException(e)
////            } catch (e: IllegalBlockSizeException) {
////                throw ValidationException(e)
////            } catch (e: BadPaddingException) {
////                throw ValidationException(e)
////            } catch (e: InvalidKeyException) {
////                throw ValidationException(e)
////            } catch (e: NoSuchAlgorithmException) {
////                throw ValidationException(e)
////            } catch (e: NoSuchProviderException) {
////                throw ValidationException(e)
////            } catch (e: NoSuchPaddingException) {
////                throw ValidationException(e)
////            } catch (e: InvalidAlgorithmParameterException) {
////                throw ValidationException(e)
////            }
////
////        }
//
//        @Throws(ValidationException::class)
//        fun create(seed: ByteArray): ExtendedKey {
//            try {
//                val mac = Mac.getInstance("HmacSHA512", "BC")
//                val seedkey = SecretKeySpec(BITCOIN_SEED, "HmacSHA512")
//                mac.init(seedkey)
//                val lr = mac.doFinal(seed)
//                val l = Arrays.copyOfRange(lr, 0, 32)
//                val r = Arrays.copyOfRange(lr, 32, 64)
//                val m = BigInteger(1, l)
//                if (m.compareTo(curve.n) >= 0) {
//                    throw ValidationException("This is rather unlikely, but it did just happen")
//                }
//                val keyPair = ECKeyPair(l, true)
//                return ExtendedKey(keyPair, r, 0, 0, 0)
//            } catch (e: NoSuchAlgorithmException) {
//                throw ValidationException(e)
//            } catch (e: NoSuchProviderException) {
//                throw ValidationException(e)
//            } catch (e: InvalidKeyException) {
//                throw ValidationException(e)
//            }
//
//        }
//
////        fun createNew(): ExtendedKey {
////            val key = ECKeyPair.createNew(true)
////            val chainCode = ByteArray(32)
////            rnd.nextBytes(chainCode)
////            return ExtendedKey(key, chainCode, 0, 0, 0)
////        }
//
//        private val xprv = byteArrayOf(0x04, 0x88.toByte(), 0xAD.toByte(), 0xE4.toByte())
//        private val xpub = byteArrayOf(0x04, 0x88.toByte(), 0xB2.toByte(), 0x1E.toByte())
//        private val tprv = byteArrayOf(0x04, 0x35.toByte(), 0x83.toByte(), 0x94.toByte())
//        private val tpub = byteArrayOf(0x04, 0x35.toByte(), 0x87.toByte(), 0xCF.toByte())
//
////        @Throws(ValidationException::class)
////        fun parse(serialized: String): ExtendedKey {
////            val data = ByteUtils.fromBase58WithChecksum(serialized)
////            if (data.size != 78) {
////                throw ValidationException("invalid extended key")
////            }
////            val type = Arrays.copyOf(data, 4)
////            val hasPrivate: Boolean
////            if (Arrays.areEqual(type, xprv) || Arrays.areEqual(type,
////                    tprv
////                )) {
////                hasPrivate = true
////            } else if (Arrays.areEqual(type, xpub) || Arrays.areEqual(type,
////                    tpub
////                )) {
////                hasPrivate = false
////            } else {
////                throw ValidationException("invalid magic number for an extended key")
////            }
////
////            val depth = data[4] and 0xff
////
////            var parent = data[5] and 0xff
////            parent = parent shl 8
////            parent = parent or (data[6] and 0xff)
////            parent = parent shl 8
////            parent = parent or (data[7] and 0xff)
////            parent = parent shl 8
////            parent = parent or (data[8] and 0xff)
////
////            var sequence = data[9] and 0xff
////            sequence = sequence shl 8
////            sequence = sequence or (data[10] and 0xff)
////            sequence = sequence shl 8
////            sequence = sequence or (data[11] and 0xff)
////            sequence = sequence shl 8
////            sequence = sequence or (data[12] and 0xff)
////
////            val chainCode = Arrays.copyOfRange(data, 13, 13 + 32)
////            val pubOrPriv = Arrays.copyOfRange(data, 13 + 32, data.size)
////            val key: Key
////            if (hasPrivate) {
////                key = ECKeyPair(BigInteger(1, pubOrPriv), true)
////            } else {
////                key = ECPublicKey(pubOrPriv, true)
////            }
////            return ExtendedKey(key, chainCode, depth, parent, sequence)
////        }
//    }
//}
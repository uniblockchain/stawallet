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
//import org.bouncycastle.util.Arrays
//import org.web3j.crypto.ECKeyPair
//
//class ECPublicKey(private var pub: ByteArray, override var isCompressed: Boolean) : Key {
//
//    override val address: ByteArray
//        get() = Hash.keyHash(pub)
//
//    override val private: ByteArray?
//        get() = null
//
//    override val public: ByteArray
//        get() = Arrays.clone(pub)
//
//    @Throws(CloneNotSupportedException::class)
//    override fun clone(): ECPublicKey {
//        val c = super.clone() as ECPublicKey
//        c.pub = Arrays.clone(pub)
//        return c
//    }
//
//    @Throws(ValidationException::class)
//    override fun sign(data: ByteArray): ByteArray {
//        throw ValidationException("Can not sign with public key")
//    }
//
//    override fun verify(hash: ByteArray, signature: ByteArray): Boolean {
//        return ECKeyPair.verify(hash, signature, pub)
//    }
//
//}

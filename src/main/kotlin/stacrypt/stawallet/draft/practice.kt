package stacrypt.stawallet.draft

import org.kethereum.bip32.model.Seed
import org.kethereum.bip32.toKey
import org.kethereum.crypto.toCredentials
import org.kethereum.encodings.encodeToBase58String
import org.kethereum.encodings.encodeToBase58WithChecksum
import org.kethereum.extensions.toBigInteger
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.extensions.toMinimalByteArray
import org.kethereum.hashes.ripemd160
import org.kethereum.hashes.sha256
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toHexString
import stacrypt.stawallet.bitcoin.toBitcoinAddress
import stacrypt.stawallet.bitcoin.toBitcoinWif
import stacrypt.stawallet.getCompressedPublicKey
import stacrypt.stawallet.ripple.toRippleAddress
import stacrypt.stawallet.ripple.toRippleSeed

fun main(args: Array<String>) {
//    val m = MnemonicWords("liar orient siege thumb try certain next fit weird simple divorce circle")
//    val s =
//        Seed("ce2e130ce9d42afd52fd69c218a7cb8412b2aaabc6d98d2e28883cfbca38ca777f6ddb37ec8b1df71a4b3b2a1999b5a74811c187f02ae650709ea0dd0484a806".toByteArray())
//    val s = Seed()

    // BIP39: `enhance before small`
    val seedHexStr =
        "5c6e14e58ad94121498ea9535795967a7b0339a7e3206fb2c9e52de0bb8c76dfd2e783435cbded4fc9939720386dee90db32b36bd56b85750c4d6825f8cc2e8a"
    val s = Seed(seedHexStr.hexToByteArray())
    var publicKey = s.toKey("m/44'/0'/0'/0/0").keyPair.publicKey.key.toByteArray()
//    val addr = pk.toBitcoinAddress(0)
//    val wif = s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(64).toBitcoinWif(0x80)
//
//    println()
//    println(s.toKey("m/44'/0'/0'/0").serialize(true))
//    println(s.toKey("m/44'/0'/0'/0").serialize(false))
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.publicKey.key.toHexStringNoPrefix())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toHexStringNoPrefix())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toHexStringNoPrefix().toUpperCase())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toByteArray().toHexString("").toUpperCase())
//    println((s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32) + 0x01.toMinimalByteArray()).toHexString("").toUpperCase())
//    val y = (s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32) + 0x01.toMinimalByteArray())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.publicKey.key.toByteArray().toBitcoinAddress(0))
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.publicKey.key.toHexStringNoPrefix())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.toCredentials().address)
//    println()
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toHexStringNoPrefix())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toHexStringNoPrefix())
//
//    println()
//    println(addr)
//    println(wif)
//
//    println(( 0x80.toMinimalByteArray() + s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)))
//    println(( 0x80.toMinimalByteArray() + s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)).toHexString(""))
//    println(( 0x80.toMinimalByteArray() + s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)).encodeToBase58String())
//    println(( 0x80.toMinimalByteArray() + s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)).encodeToBase58WithChecksum())
//    println(( 0x80.toMinimalByteArray() + s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)).size)
//    var x = ( 0x80.toMinimalByteArray() + s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32))
//    println(x + x.sha256().sha256().take(4))
//    println((x + x.sha256().sha256().take(4)).toHexString(""))
//    println((x + x.sha256().sha256().take(4)).size)
//    println((x + x.sha256().sha256().take(4)).encodeToBase58String())
//    println((x + x.sha256().sha256().take(4)).encodeToBase58WithChecksum())
//    println("0C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D")
//    x = s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toByteArray()
//    println(x.toHexString(""))
//    x = s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)
//    println(x.toHexString(""))
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toHexStringNoPrefix())
////    x = "0C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D".hexToByteArray()
//    x = (0x80.toMinimalByteArray() + x)
//    println(x.encodeToBase58WithChecksum())
//    x = (x + x.sha256().sha256().take(4))
//    println(x.toHexString(""))
//    println(x.encodeToBase58String())
//    println((0x80.toMinimalByteArray() + y).encodeToBase58WithChecksum())
//    println(s.toKey("m/44'/0'/0'/0/0").keyPair.privateKey.key.toBitcoinWif(0x80))
//    println(x + x.sha256().sha256().take(4))
//    println((x + x.sha256().sha256().take(4)).toHexString(""))
//    println((x + x.sha256().sha256().take(4)).size)
//    println((x + x.sha256().sha256().take(4)).encodeToBase58String())
//    println((x + x.sha256().sha256().take(4)).encodeToBase58WithChecksum())

//    publicKey = s.toKey("m/44'/144'/0'/0/0").keyPair.publicKey.key.toByteArray()
//    val ser = publicKey.getCompressedPublicKey().toHexString("")
//    if(ser != "0305ca5f147c7fdd007cbd8853baad6c1c7aae6944dac8657a2b85373b38363abb")
//        throw Exception("$ser != 0305ca5f147c7fdd007cbd8853baad6c1c7aae6944dac8657a2b85373b38363abb")
    val privateKey = s.toKey("m/44'/144'/0'/0/0").keyPair.privateKey.key.toBytesPadded(32)
    var ser = privateKey.toHexString("")
    println(ser)
    println(ser.hexToByteArray().ripemd160().toHexString(""))
    var xxx = intArrayOf(207, 45, 227, 120, 251, 221, 126, 46, 232, 125, 72, 109, 251, 90, 123, 255)
    var yyy = xxx.foldIndexed(ByteArray(xxx.size)) { i, a, v -> a.apply { set(i, v.toByte()) } }
    println(yyy.toHexString(""))
//    ser = "D78B9735C3F26501C7337B8A5727FD53A6EFDBC6AA55984F098488561F985E23"
    ser = yyy.toHexString("")
    println(ser.hexToByteArray().toRippleSeed())
    println("sn259rEFXrQrWyx3Q7XneWcwV6dfL")

}
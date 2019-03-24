package stacrypt.stawallet

import org.kethereum.bip32.model.Seed
import org.kethereum.bip32.toKey
import org.kethereum.crypto.CURVE
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.toHex
import org.kethereum.extensions.toBigInteger
import org.kethereum.extensions.toBytesPadded
import org.kethereum.hashes.sha256
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PUBLIC_KEY_SIZE
import org.kethereum.model.PublicKey


// FIXME: Highly dangerous because of `hotSeed` variable accessibility (e.g. using reflection)
abstract class SecretProvider(internal val accountId: Int) {

    companion object {
        const val MAGIC_NUMBER = 44
    }

    abstract val coinType: Int

    protected abstract val hotSeed: ByteArray
    abstract val coldAddress: String

    fun makePath(index: Int, change: Int?) =
        "m/$MAGIC_NUMBER'/$coinType'/$accountId'/${if (change != null) "$change/$index" else "$index"}"


    fun getHotAddress(index: Int, change: Int?): String {
        return getHotAddress(makePath(index, change))
    }

    private fun getHotAddress(fullPath: String): String {
        return Seed(hotSeed).toKey(fullPath).serialize(true)
    }

    fun getHotPublicKey(fullPath: String): ByteArray {
        return Seed(hotSeed).toKey(fullPath).keyPair.publicKey.key.toByteArray()
    }

    fun getHotPublicKeyObject(fullPath: String): PublicKey {
        return Seed(hotSeed).toKey(fullPath).keyPair.publicKey
    }

    fun getHotPrivateKey(fullPath: String): ByteArray {
        return Seed(hotSeed).toKey(fullPath).keyPair.privateKey.key.toBytesPadded(32)
    }

    fun getHotKeyPair(fullPath: String): ECKeyPair = Seed(hotSeed).toKey(fullPath).keyPair


    fun signTxWithHotPrivateKey(message: ByteArray, fullPath: String): String {
        return Seed(hotSeed).toKey(fullPath).keyPair.signMessage(
            message.sha256().sha256()
        ).toHex()
    }

}


class SimpleSecretProvider(
    private val hotSeedGenerator: () -> ByteArray,
    override var coinType: Int,
    accountId: Int,
    override val coldAddress: String
) :
    SecretProvider(accountId) {
    override val hotSeed get () = hotSeedGenerator.invoke()
}

fun ByteArray.getCompressedPublicKey(): ByteArray {
    //add the uncompressed prefix
    val ret = this.toBigInteger().toBytesPadded(PUBLIC_KEY_SIZE + 1)
    ret[0] = 4
    val point = CURVE.decodePoint(ret)
    return point.encoded(true)
}

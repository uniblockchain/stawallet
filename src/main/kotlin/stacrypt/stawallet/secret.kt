package stacrypt.stawallet

import com.typesafe.config.Config
import org.kethereum.bip32.model.Seed
import org.kethereum.bip32.toKey
import org.kethereum.crypto.CURVE
import org.kethereum.crypto.api.ec.EllipticCurveSigner
import org.kethereum.crypto.model.PUBLIC_KEY_SIZE
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.toHex
import org.kethereum.encodings.encodeToBase58WithChecksum
import org.kethereum.extensions.toBigInteger
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toMinimalByteArray
import org.kethereum.hashes.sha256
import org.kethereum.ripemd160.calculateRIPEMD160
import org.walleth.khex.hexToByteArray


// FIXME: Highly dangerous because of `hotSeed` variable accessibility (e.g. using reflection)
abstract class SecretProvider(private val walletNumber: Int = 0) {

    companion object {
        const val MAGIC_NUMBER = 44
    }

    abstract val coinType: Int

    protected abstract var hotSeed: ByteArray
    abstract var coldAddress: String

    fun makePath(index: Int, change: Int?) =
        "m/$MAGIC_NUMBER'/$coinType'/$walletNumber'/${if (change != null) "$change/$index" else "$index"}"


    fun getHotAddress(index: Int, change: Int?): String {
        return getHotAddress(makePath(index, change))
    }

    private fun getHotAddress(fullPath: String): String {
        return Seed(hotSeed).toKey(fullPath).serialize(true)
    }

    fun getHotPublicKey(fullPath: String): ByteArray {
        return Seed(hotSeed).toKey(fullPath).keyPair.publicKey.key.toByteArray()
    }

    fun signTxWithHotPrivateKey(message: ByteArray, fullPath: String): String {
        return Seed(hotSeed).toKey(fullPath).keyPair.signMessage(
            message.sha256().sha256()
        ).toHex()
    }

}


class ConfigSecretProvider(val config: Config, override var coinType: Int) : SecretProvider(0) {


    override var hotSeed: ByteArray
        get() = config.getString("seed").hexToByteArray()
        set(value) {
            throw Exception()
        }

    override var coldAddress: String
        get() = config.getString("coldAddress")
        set(value) {
            throw Exception()
        }
}

fun ByteArray.getCompressedPublicKey(): ByteArray {
    //add the uncompressed prefix
    val ret = this.toBigInteger().toBytesPadded(PUBLIC_KEY_SIZE + 1)
    ret[0] = 4
    val point = CURVE.decodePoint(ret)
    return point.encoded(true)
}

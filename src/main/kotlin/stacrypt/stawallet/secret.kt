package stacrypt.stawallet

import com.typesafe.config.Config
import org.kethereum.bip32.model.Seed
import org.kethereum.bip32.toKey
import org.walleth.khex.hexToByteArray


abstract class SecretProvider(private val walletNumber: Int = 0) {

    companion object {
        const val MAGIC_NUMBER = 44
    }

    abstract val coinType: Int
    abstract var hotSeed: ByteArray
    abstract var coldAddress: String

    private fun makePath(index: Int, change: Int?) =
        "m/$MAGIC_NUMBER'/$coinType'/$walletNumber'/${if (change != null) "$index/$change" else "$index"}"


    fun getHotAddress(index: Int, change: Int?) {
        Seed(hotSeed).toKey(makePath(index, change)).serialize(true)
    }

    fun getHotPrivateKey(index: Int, change: Int?) {
        Seed(hotSeed).toKey(makePath(index, change)).serialize(false)
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
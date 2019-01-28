package stacrypt.stawallet

import stacrypt.stawallet.bitcoin.BitcoinWallet
import java.util.logging.Level
import java.util.logging.Logger

abstract class Wallet(val name: String, val daemon: WalletDaemon, private val secretProvider: SecretProvider) {

    private val logger = Logger.getLogger("Wallet $name")
    abstract val coin: String
    abstract val storage: RedisStorage

    companion object Factory {

        fun initFromConfig(): ArrayList<Wallet> {
            val all = ArrayList<Wallet>()
            for (wc in config.getObject("wallets").toList()) {

                when (config.getString("wallets.${wc.first}.cryptocurrency")) {
                    "bitcoin" -> all.add(BitcoinWallet(wc.first, config.getConfig("wallets.${wc.first}")))
//                    "litecoin" -> all.add(LitecoinWallet(address, xPrv))
//                    "ethereum" -> all.add(EthereumWallet(address, xPrv))
//                    "ripple" -> all.add(RippleWallet(address, xPrv))
//                    else -> throw RuntimeException("Unsupported coin ${wc.first}!!!")
                }

                Logger.getLogger("Wallet Factory").log(Level.SEVERE, "wallet found: ${wc.first}")
            }
            return all
        }
    }


    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit
    abstract suspend fun sendTo(address: String, amount: Long): Unit
}
package stacrypt.stawallet

import stacrypt.stawallet.bitcoin.BitcoinWallet
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.DepositTable
import stacrypt.stawallet.model.InvoiceDao
import java.util.logging.Level
import java.util.logging.Logger

abstract class Wallet(val name: String, val secretProvider: SecretProvider) {

    private val logger = Logger.getLogger("Wallet $name")
    //    abstract val storage: RedisStorage
    abstract val daemon: WalletDaemon

    companion object Factory {

        fun initFromConfig(): ArrayList<Wallet> {
            val all = ArrayList<Wallet>()
            for (wc in config.getObject("wallets").toList()) {

                when (config.getString("wallets.${wc.first}.cryptocurrency")) {
                    "bitcoin" -> all.add(BitcoinWallet(wc.first, config.getConfig("wallets.${wc.first}")))
//                    "litecoin" -> all.add(LitecoinWallet(address, xPrv))
//                    "ethereum" -> all.add(EthereumWallet(address, xPrv))
//                    "ripple" -> all.add(RippleWallet(address, xPrv))
//                    else -> throw RuntimeException("Unsupported wallet ${wc.first}!!!")
                }

                Logger.getLogger("Wallet Factory").log(Level.SEVERE, "wallet found: ${wc.first}")
            }
            return all
        }
    }


    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit
    abstract suspend fun lastUsableInvoice(user: String): InvoiceDao?
    abstract suspend fun invoiceDeposits(invoiceId: Int): List< DepositDao>
    abstract suspend fun issueNewInvoice(user: String): InvoiceDao
    abstract suspend fun sendTo(address: String, amount: Long): Unit
}
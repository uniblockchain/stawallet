package stacrypt.stawallet

import io.ktor.config.tryGetString
import stacrypt.stawallet.bitcoin.BitcoinWallet
import stacrypt.stawallet.bitcoin.NETWORK_MAINNET
import stacrypt.stawallet.bitcoin.NETWORK_TESTNET_3
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.DepositTable
import stacrypt.stawallet.model.InvoiceDao
import java.security.InvalidParameterException
import java.util.logging.Level
import java.util.logging.Logger

abstract class Wallet(val name: String, val secretProvider: SecretProvider, val network: String) {

    private val logger = Logger.getLogger("Wallet $name")
    //    abstract val storage: RedisStorage
    abstract val daemon: WalletDaemon

    companion object Factory {

        fun initFromConfig(): ArrayList<Wallet> {
            val all = ArrayList<Wallet>()
            for (wc in config.getObject("wallets").toList()) {

                val network = config.tryGetString("wallets.${wc.first}.network")
                when (config.getString("wallets.${wc.first}.cryptocurrency")) {
                    "bitcoin" -> all.add(
                        BitcoinWallet(
                            wc.first,
                            config.getConfig("wallets.${wc.first}"),
                            when (network) {
                                "mainnet" -> NETWORK_MAINNET
                                "testnet3" -> NETWORK_TESTNET_3
                                else -> throw InvalidParameterException("Network '$network' is not supported")
                            }
                        )
                    )
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


    /**
     * Url to redirect user to a third-party blockchain explorer
     */
    abstract fun blockchainExplorerTxLink(txId: String): String?

    abstract val requiredConfirmations: Int

    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit

    /**
     * @Return the last usable invoice
     */
    abstract suspend fun lastUsableInvoice(user: String): InvoiceDao?

    /**
     * @Return the deposit records which were recorded for a specific invoiceId
     */
    abstract suspend fun invoiceDeposits(invoiceId: Int): List<DepositDao>

    abstract suspend fun issueNewInvoice(user: String): InvoiceDao
    abstract suspend fun sendTo(address: String, amountToSend: Long): Any
}
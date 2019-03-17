package stacrypt.stawallet

import io.ktor.config.tryGetString
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.walleth.khex.hexToByteArray
import stacrypt.stawallet.bitcoin.*
import stacrypt.stawallet.ethereum.EthereumWallet
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.DepositTable
import stacrypt.stawallet.model.InvoiceDao
import stacrypt.stawallet.model.WalletDao
import java.math.BigInteger
import java.security.InvalidParameterException
import java.util.logging.Level
import java.util.logging.Logger

data class NotEnoughFundException(val wallet: String, val amountToPay: Any = 0L) :
    Exception("wallet $wallet does NOT have enough money to pay $amountToPay")

data class TransactionPushException(val wallet: String, val transaction: Any, val error: String) :
    Exception("wallet $wallet did not accept this transaction $transaction because of: $error")

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
                    "BTC" -> all.add(
                        BitcoinWallet(
                            name = wc.first,
                            network = when (network) {
                                "mainnet" -> NETWORK_MAINNET
                                "testnet3" -> NETWORK_TESTNET_3
                                else -> throw InvalidParameterException("Network '$network' is not supported")
                            },
                            requiredConfirmations = config.getInt("wallets.${wc.first}.requiredConfirmations"),
                            secretProvider = SimpleSecretProvider(
                                hotSeedGenerator = { config.getString("secrets.hotSeed").hexToByteArray() },
                                accountId = config.getInt("wallets.${wc.first}.accountId"),
                                coinType = BitcoinWallet.coinType(network),
                                coldAddress = config.getString("wallets.${wc.first}.coldAddress")
                            )
                        )
                    )
//                    "litecoin" -> all.add(LitecoinWallet(address, xPrv))
                    "ETH" -> all.add(
                        EthereumWallet(
                            name = wc.first,
                            network = network ?: throw InvalidParameterException("No network specified"),
                            requiredConfirmations = config.getInt("wallets.${wc.first}.requiredConfirmations"),
                            secretProvider = SimpleSecretProvider(
                                hotSeedGenerator = { config.getString("secrets.hotSeed").hexToByteArray() },
                                accountId = config.getInt("wallets.${wc.first}.accountId"),
                                coinType = EthereumWallet.coinType(),
                                coldAddress = config.getString("wallets.${wc.first}.coldAddress")
                            )
                        )
                    )
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
    open suspend fun invoiceDeposits(invoiceId: Int): List<DepositDao> =
        DepositDao.wrapRows(
            DepositTable.select { DepositTable.invoice eq InvoiceDao[invoiceId].id }.orderBy(DepositTable.id, false)
        ).toList()

    abstract suspend fun issueNewInvoice(user: String): InvoiceDao
    abstract suspend fun sendTo(address: String, amountToSend: BigInteger, tag: Any?): Any


    var blockchainWatcher: BaseBlockchainWatcher? = null
    fun isBlockchainWatcherActive() = blockchainWatcher != null // FIXME
    abstract fun startBlockchainWatcher(): BaseBlockchainWatcher
    abstract fun stopBlockchainWatcher()

    abstract fun initializeToDb(force: Boolean = false): WalletDao

}

interface BaseBlockchainWatcher
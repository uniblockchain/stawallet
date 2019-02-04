package stacrypt.stawallet.bitcoin

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon
import stacrypt.stawallet.model.*
import java.lang.Exception

object bitcoind : WalletDaemon() {
    override var status: DaemonState
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    val rpcClient: BitcoinRpcClient by lazy { BitcoinRpcClientFactory.createBitcoinClientWithDefaultConfig() }

    /**
     * Satoshis per bytes
     */
    suspend fun fairTxFeeRate(): Long? = withContext(Dispatchers.Default) {
        rpcClient.estimateSmartFee(6).feerate?.btcToSat()
    }

    fun startWatcher() {
        runBlocking(BicoinDaemonWatcher.retrieveDispatcher()) {

        }
    }

}

const val WATCHER_TRANSACTION_DELAY = 1_000L
const val WATCHER_BLOCK_DELAY = 10_000L

suspend fun startBitcoindWatcher() {
    coroutineScope {
        supervisorScope {}


        val transactionJob = launch {
            while (true) {
                delay(WATCHER_TRANSACTION_DELAY)


                (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->
                    tx.vout
                        ?.map { vout ->
                            var associatedAddress: AddressDao? = null
                            transaction {
                                associatedAddress = AddressTable
                                    .select {
                                        (AddressTable.isActive eq true) and (AddressTable.provision eq (vout.scriptPubKey?.addresses?.lastOrNull() as String))
                                    }.lastOrNull()?.run { AddressDao.wrapRow(this) }
                            }
                            Pair(associatedAddress, vout)
                        }
                        ?.filter { it.first != null }
                        ?.forEach {
                            // Insert new UTXO
                            try {
                                transaction {
                                    if (UtxoTable.select { (UtxoTable.txid eq tx.hash!!) and (UtxoTable.vout eq it.second.n!!.toInt()) }.count() == 0) {
                                        // This is new UTXO!
                                        UtxoDao.new {
                                            this.address = it.first!!
                                            this.wallet = it.first!!.wallet
                                            this.txid = tx.hash!!
                                            this.vout = it.second.n!!.toInt()
                                            this.amount = it.second.value!!.toLong()
                                        }
                                    }
                                }
                            } catch (e: Exception) {

                            }


                            // Insert new Deposit
                            try {
                                transaction {
                                    // Find the related invoice
                                    val relatedInvoice = InvoiceTable
                                        .select { InvoiceTable.wallet eq it.first!!.wallet.id }
                                        .andWhere { InvoiceTable.address eq it.first!!.id}
                                        .andWhere { InvoiceTable.expiration.isNull() or (InvoiceTable.expiration greater DateTime.now())}
                                        .lastOrNull()?.run { InvoiceDao.wrapRow(this) }

                                    if (DepositTable.select { DepositTable.invoice eq relatedInvoice!!.id}.count() == 0) {
                                        // This is new UTXO!
                                        UtxoDao.new {
                                            this.address = it.first!!
                                            this.wallet = it.first!!.wallet
                                            this.txid = tx.hash!!
                                            this.vout = it.second.n!!.toInt()
                                            this.amount = it.second.value!!.toLong()
                                        }
                                    }
                                }
                            } catch (e: Exception) {

                            }
                        }
                }
            }
        }
    }
}

class BicoinDaemonWatcher(private val daemon: WalletDaemon) {

    companion object {
        fun retrieveDispatcher() = newSingleThreadContext("bitcoind-watcher")
    }

    private val dispatcher: ExecutorCoroutineDispatcher =
        private
    val supervisorJob: Job
    private val transactionWatcherJob: Job
    private val blockWatcherJob: Job

    init {

    }

}
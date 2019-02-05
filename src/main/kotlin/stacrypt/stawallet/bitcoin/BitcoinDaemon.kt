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

    fun retrieveDispatcher() = newSingleThreadContext("bitcoind-watcher")

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
        runBlocking(retrieveDispatcher()) {

        }
    }

    const val WATCHER_TRANSACTION_DELAY = 1_000L
    const val WATCHER_BLOCK_DELAY = 10_000L


    suspend fun startWatchers() {
        coroutineScope {
            supervisorScope {}

            val blockWatcherJob = startBlockWatcherJob(this)
            val transactionWatcherJob = startBlockWatcherJob(this)
        }

    }

    private fun startTransactionWatcherJob(scope: CoroutineScope) = scope.launch {
        delay(WATCHER_TRANSACTION_DELAY)
        (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->

        }
    }

    /**
     * Here we are serching for any associated address in our watching addresses for each incoming transaction utxo.
     */
    private fun findAssociatedAddress(vout: TransactionOutput) = transaction {
        AddressTable
            .select {
                (AddressTable.isActive eq true) and (AddressTable.provision eq (vout.scriptPubKey?.addresses?.lastOrNull() as String))
            }.lastOrNull()?.run { AddressDao.wrapRow(this) }
    }

    /**
     * Here we add a new confirmaton event whenever a new block found.
     *
     * Since the transaction fully confirmed, regardless of the previous event of the transaction, we'll insert a new
     * confirmation event for the transaction and related invoice.
     */
    private fun addNewConfirmationEvent(
        address: AddressDao,
        transaction: Transaction,
        transactionOutput: TransactionOutput
    ) {

    }

    /**
     * Here we make a new utxo record, because we are SURE that it is a fully confirmed utxo.
     *
     * Note: We'll make a UTXO record per incoming transaction inputs, and the index of it. Regardless of the
     * address of the sender. For example if we have 2 input from a single sender, we will generate 2 separated UTXOs.
     *
     */
    private fun insertNewUtxo(address: AddressDao, transaction: Transaction, transactionOutput: TransactionOutput) =
        transaction {
            if (UtxoTable.select { (UtxoTable.txid eq transaction.hash!!) and (UtxoTable.vout eq transactionOutput.n!!.toInt()) }.count() == 0) {
                // This is new UTXO!
                UtxoDao.new {
                    this.address = address
                    this.wallet = address.wallet
                    this.txid = transaction.hash!!
                    this.vout = transactionOutput.n!!.toInt()
                    this.amount = transactionOutput.value!!.toLong()
                }
            }

        }

    /**
     * Here we make a new deposit record, because we are SURE that it is a fully confirmed transaction.
     *
     * Note: The saved deposits are belongs to a specific invoice. If we couldn't find any related invoice, we will not
     * record this deposit.
     *
     * Note 2: Before calling this method, your data should be grouped by invoices. For example if we have 2 input from
     * a single sender, we will not save it. You should sum all amounts of a specific invoice in a specific tx, then
     * call this method. It means we have a unique constraint by transaction hash and invoice.
     *
     */
    private fun insertNewDeposit(address: AddressDao, transaction: Transaction, transactionOutput: TransactionOutput) =
        transaction {
            // Find the related invoice
            val relatedInvoice = InvoiceTable
                .select { InvoiceTable.wallet eq address.wallet.id }
                .andWhere { InvoiceTable.address eq address.id }
                .andWhere { InvoiceTable.expiration.isNull() or (InvoiceTable.expiration greater DateTime.now()) }
                .lastOrNull()?.run { InvoiceDao.wrapRow(this) }

            if (DepositTable.select { DepositTable.invoice eq relatedInvoice!!.id }.count() == 0) {
                // This is new UTXO!
                UtxoDao.new {
                    this.address = address
                    this.wallet = address.wallet
                    this.txid = transaction.hash!!
                    this.vout = transactionOutput.n!!.toInt()
                    this.amount = transactionOutput.value!!.toLong()
                }
            }
        }


    private fun startBlockWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {
            delay(WATCHER_BLOCK_DELAY)


            (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->
                tx.vout
                    ?.map { vout -> Pair(findAssociatedAddress(vout), vout) }
                    ?.filter { it.first != null }
                    ?.forEach {
                        // Insert new UTXO
                        try {
                            insertNewUtxo(it.first!!, tx, it.second)
                        } catch (e: Exception) {

                        }


                        // Insert new Deposit
                        try {
                            insertNewDeposit(it.first!!, tx, it.second)
                        } catch (e: Exception) {

                        }
                    }
            }
        }
    }


}


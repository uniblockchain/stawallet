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
import stacrypt.stawallet.config
import stacrypt.stawallet.model.*
import stacrypt.stawallet.sumByLong
import java.lang.Exception
import kotlin.math.max

object bitcoind : WalletDaemon() {

    private val blockchainWatchers: ArrayList<Pair<CoroutineScope, BitcoinBlockchainWatcher>> = ArrayList()

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

    fun startBlockchainWatcher(walletName: String, requiresConfirmations: Int) {
        runBlocking {
            supervisorScope {
                val watcher = BitcoinBlockchainWatcher(walletName, requiresConfirmations)
                blockchainWatchers.add(Pair(this, watcher))
                watcher.startWatcher()
            }

        }
    }

}

class BitcoinBlockchainWatcher(val walletName: String, val requiresTransactions: Int) {

    companion object {
        const val WATCHER_TRANSACTION_DELAY = 1_000L
        const val WATCHER_BLOCK_DELAY = 10_000L
    }

    val dispatcher: CoroutineDispatcher = newSingleThreadContext("$walletName-watcher")

    private fun startMempoolWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {
            delay(WATCHER_TRANSACTION_DELAY)
            (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->
                processTransaction(tx, null, requiresTransactions)
            }
        }
    }


    private fun startBlockWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {
            delay(WATCHER_BLOCK_DELAY)

            (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->
                processTransaction(tx, 0, requiresTransactions)
            }
        }
    }

    fun startWatcher() {
        runBlocking(this.dispatcher) {
            val blockWatcherJob = startBlockWatcherJob(this)
            val mempoolWatcherJob = startMempoolWatcherJob(this)

        }
    }

    /**
     * Here we are serching for any associated address in our watching addresses for each incoming transaction utxo.
     */
    private fun findAssociatedAddress(vout: TransactionOutput) = transaction {
        AddressTable
            .select { AddressTable.isActive eq true }
            .andWhere { AddressTable.wallet eq WalletDao[walletName].id }
            .andWhere { AddressTable.provision eq (vout.scriptPubKey?.addresses?.lastOrNull() as String) }
            .lastOrNull()?.run { AddressDao.wrapRow(this) }
    }

    /**
     * Here we add a new confirmaton event whenever a new block found.
     *
     * Since the transaction fully confirmed, regardless of the previous event of the transaction, we'll insert a new
     * confirmation event for the transaction and related invoice.
     */
    private fun insertNewConfirmationEvent(
        address: AddressDao,
        transaction: Transaction,
        transactionOutput: TransactionOutput
    ) {

    }

    private fun findProof(address: AddressDao, transaction: Transaction, transactionOutput: TransactionOutput): ProofDao? =
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
            } else {
                // We are reading a blockchain again. This is dangerous!!!

            }

        }

    private fun insertNewProof(address: AddressDao, transaction: Transaction, transactionOutput: TransactionOutput): ProofDao? =
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
            } else {
                // We are reading a blockchain again. This is dangerous!!!

            }

        }

    /**
     * Here we make a new utxo record, because we are SURE that it is a fully confirmed utxo.
     *
     * Note: We'll make a UTXO record per incoming transaction inputs, and the index of it. Regardless of the
     * address of the sender. For example if we have 2 input from a single sender, we will generate 2 separated UTXOs.
     *
     */
    private fun insertNewUtxo(address: AddressDao, transaction: Transaction, transactionOutput: TransactionOutput, proof: ProofDao) =
        transaction {
            if (UtxoTable.select { (UtxoTable.txid eq transaction.hash!!) and (UtxoTable.vout eq transactionOutput.n!!.toInt()) }.count() == 0) {
                // This is new UTXO!
                UtxoDao.new {
                    this.address = address
                    this.wallet = address.wallet
                    this.txid = transaction.hash!!
                    this.vout = transactionOutput.n!!.toInt()
                    this.amount = transactionOutput.value!!.toLong()
                    this.discoveryProof = proof
                }
            } else {
                // We are reading a blockchain again. This is dangerous!!!

            }

        }

    private fun findRelatedInvoice(address: AddressDao): InvoiceDao? = transaction {
        InvoiceTable
            .select { InvoiceTable.wallet eq address.wallet.id }
            .andWhere { InvoiceTable.address eq address.id }
            .andWhere { InvoiceTable.expiration.isNull() or (InvoiceTable.expiration greater DateTime.now()) }
            .lastOrNull()?.run { InvoiceDao.wrapRow(this) }
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
    private fun insertNewDeposit(
        relatedInvoice: InvoiceDao?,
        transactionHash: String,
        amount: Long,
        feeAmount: Long = 0L,
        proof: ProofDao
    ) =
        transaction {
            // Find the related invoice
            when {
                relatedInvoice == null -> {
                    // It is a anonymous receiving amount. We could'nt proceed this deposit.
                    // Just throw an exception to inform the admin
                    // TODO Report to the boss
                }
                DepositTable.leftJoin(ProofTable)
                    .select { DepositTable.invoice eq relatedInvoice.id }
                    .andWhere { ProofTable.txHash eq transactionHash }
                    .count() == 0 -> // This is new UTXO!
                    DepositDao.new {
                        this.grossAmount = amount
                        this.netAmount = max(0, amount - feeAmount)
                        this.invoice = relatedInvoice
                        this.proof = proof
                    }
                else -> {
                    // We are reading a blockchain again. This is dangerous!!!
                    // TODO Report to the boss
                }
            }
        }

    private fun processConfirmedTransaction(tx: Transaction) {

        // FIXME: Handle Utxo's isSpent

        var proof: ProofDao? = null
        try {
            proof = findProof(it.first!!, tx, it.second)
            if(proof == null) proof = insertNewProof(it.first!!, tx, it.second)

            // TODO: Update proof

        } catch (e: Exception) {

            // TODO Report to the boss
        }

        tx.vout
            ?.asSequence()
            ?.map { vout -> Pair(findAssociatedAddress(vout), vout) }
            ?.filter { it.first != null }
            ?.map {

                /**
                 * Insert new UTXOs
                 */

                try {
                    insertNewUtxo(it.first!!, tx, it.second)
                } catch (e: Exception) {

                    // TODO Report to the boss
                }
                it
            }
            ?.map {
                Pair(findRelatedInvoice(it.first!!), it.second)
            }?.filter {
                it.first != null
            }?.groupBy {
                it.first!!.id.value
            }?.forEach { _, v ->

                /**
                 * Insert new Deposit
                 */

                try {
                    insertNewDeposit(
                        relatedInvoice = v.firstOrNull()?.first,
                        transactionHash = tx.hash!!,
                        amount = v.sumByLong { it.second.value!!.toLong() }
                    )
                } catch (e: Exception) {

                    // TODO Report to the boss
                }

            }
    }

    private fun processUnconfirmedTransaction(tx: Transaction, confirmations: Int, confirmationsRequires: Int) {

    }

    private fun processOrphanTransaction(tx: Transaction, confirmationsRequires: Int) {

    }

    private fun processTransaction(tx: Transaction, confirmations: Int?, confirmationsRequires: Int) {
        return when {
            confirmations == null -> this.processOrphanTransaction(tx, confirmationsRequires)
            confirmations >= confirmationsRequires -> this.processConfirmedTransaction(tx)
            else -> this.processUnconfirmedTransaction(tx, confirmations, confirmationsRequires)
        }

    }


}


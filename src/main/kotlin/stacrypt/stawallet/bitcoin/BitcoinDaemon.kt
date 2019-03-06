package stacrypt.stawallet.bitcoin

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import stacrypt.stawallet.*
import stacrypt.stawallet.model.*
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
    fun fairTxFeeRate(): Long? {
        return rpcClient.estimateSmartFee(6).feerate?.btcToSat()
    }

    fun addBlockchainWatcher(
        blockchainId: Int,
        walletName: String,
        requiresConfirmations: Int
    ): BitcoinBlockchainWatcher? {
        var watcher: BitcoinBlockchainWatcher? = null
        runBlocking {
            supervisorScope {
                watcher = BitcoinBlockchainWatcher(blockchainId, walletName, requiresConfirmations)
                blockchainWatchers.add(Pair(this, watcher!!))
                watcher!!.startWatcher()
            }

        }
        return watcher
    }

    fun removeBlockchainWatcher(watcher: BitcoinBlockchainWatcher) {
        runBlocking {
            supervisorScope {
                blockchainWatchers.removeAll { it.second == watcher }
                watcher.stopWatcherAndJoin()
            }

        }
    }

}

class BitcoinBlockchainWatcher(
    private val blockchainId: Int,
    private val walletName: String,
    private val confirmationsRequires: Int
) {

    val blockWatchGap get() = config.getLong("daemons.bitcoind.watcher.blockWatchGap")
    val mempoolWatchGap get() = config.getLong("daemons.bitcoind.watcher.mempoolWatchGap")

    val dispatcher: CoroutineDispatcher = newSingleThreadContext("$walletName-watcher")

    private var mempoolWatcherJob: Job? = null
    var blockWatcherJob: Job? = null

    private fun startMempoolWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {
            delay(mempoolWatchGap)
            (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->
                processOrphanTransaction(tx)
            }
        }
    }

    private fun startBlockWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {
            delay(blockWatchGap)

            // Find out witch block height should we sync with (if there is any unsynced block)
            val currentBestBlockHeight = bitcoind.rpcClient.getBlockCount()
            val walletDao = transaction { WalletDao.findById(walletName) }!!
            val latestSyncedHeight = walletDao.latestSyncedHeight
            // TODO: Store and compare `bestblockhash` and get back in case of incompatibility
            if (currentBestBlockHeight > latestSyncedHeight) {
                // We have one or more new bocks to sync with
                // TODO: Compare database with with `previousblockhash` of the following value
                val nextBlock =
                    bitcoind.rpcClient.getBlockWithTransactions(
                        bitcoind.rpcClient.getBlockHash(latestSyncedHeight + 1)
                    )
                nextBlock.tx?.forEach { tx ->
                    processTransaction(
                        nextBlock,
                        tx
                    )
                }

                transaction {
                    // Now it's time to update previous (unconfirmed) block's confirmations
                    for (i in 0..confirmationsRequires) {
                        val b = bitcoind.rpcClient.getBlock(
                            bitcoind.rpcClient.getBlockHash(latestSyncedHeight - i)
                        )
                        b.tx?.forEach { increaseConfirmations(blockInfo = b, txHash = it) }
                    }

                    walletDao.latestSyncedHeight = latestSyncedHeight
                }

            }
        }
    }

    fun startWatcher() {
        GlobalScope.launch(this.dispatcher) {
            mempoolWatcherJob = startMempoolWatcherJob(this)
        }

        GlobalScope.launch(this.dispatcher) {
            blockWatcherJob = startBlockWatcherJob(this)
        }

    }

    suspend fun stopWatcherAndJoin() {
        if (mempoolWatcherJob?.isActive == true) mempoolWatcherJob!!.cancelAndJoin()
        if (blockWatcherJob?.isActive == true) blockWatcherJob!!.cancelAndJoin()
    }

    fun stopWatcher() {
        if (mempoolWatcherJob?.isActive == true) mempoolWatcherJob!!.cancel()
        if (blockWatcherJob?.isActive == true) blockWatcherJob!!.cancel()
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
     * This method create a new proof for the related transaction, or return the existing one if it has been created
     * in advance.
     */
    private fun inquireProof(block: BlockInfoWithTransactions, tx: Transaction): ProofDao =
        transaction {
            val q = ProofTable.select { ProofTable.blockchain eq blockchainId }
                .andWhere { ProofTable.txHash eq tx.hash!! }
                .andWhere { ProofTable.blockHash eq block.hash!! }
                .andWhere { ProofTable.blockHeight eq block.height!!.toInt() }
                .firstOrNull()

            val proof: ProofDao
            proof = if (q == null)
            // Create a new proof for this
                ProofDao.new {
                    this.blockchain = blockchain
                    this.blockHash = block.hash
                    this.blockHeight = block.height!!.toInt()
                    this.txHash = tx.hash!!
                    this.confirmationsLeft = confirmationsRequires
                }
            else
            // TODO: Check for validity
                ProofDao.wrapRow(q)

            proof
        }

    /**
     * Here we make a new utxo record, because we are SURE that it is a fully confirmed utxo.
     *
     * Note: We'll make a UTXO record per incoming transaction inputs, and the index of it. Regardless of the
     * address of the sender. For example if we have 2 input from a single sender, we will generate 2 separated UTXOs.
     *
     */
    private fun insertNewUtxo(
        address: AddressDao,
        transaction: Transaction,
        transactionOutput: TransactionOutput,
        proof: ProofDao
    ) =
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

    private fun processOrphanTransaction(tx: Transaction) {
        // TODO
    }

    private fun processTransaction(block: BlockInfoWithTransactions, tx: Transaction) {
        // FIXME: Handle Utxo's isSpent

        var proof: ProofDao? = null
        try {
            proof = inquireProof(block, tx)
            // TODO: Update proof
        } catch (e: Exception) {
            // TODO Report to the boss
            // Maybe invalid block info
        }
        proof as ProofDao

        tx.vout
            ?.asSequence()
            ?.map { vout -> Pair(findAssociatedAddress(vout), vout) }
            ?.filter { it.first != null }
            ?.map {

                /**
                 * Insert new UTXOs
                 */

                try {
                    insertNewUtxo(it.first!!, tx, it.second, proof)
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
                        amount = v.sumByLong { it.second.value!!.toLong() },
                        proof = proof
                    )
                } catch (e: Exception) {

                    // TODO Report to the boss
                }

            }

    }

    private fun increaseConfirmations(blockInfo: BlockInfo, txHash: String) {
        ProofTable.update(
            {
                (ProofTable.blockchain eq blockchainId)
                    .and(ProofTable.txHash eq txHash)
                    .and(ProofTable.blockHash eq blockInfo.hash)
                    .and(ProofTable.blockHeight eq blockInfo.height!!.toInt())
                    .and(ProofTable.confirmationsLeft greater 0)
            }
        ) {
            with(SqlExpressionBuilder) {
                it.update(ProofTable.confirmationsLeft, ProofTable.confirmationsLeft - 1)
                it[ProofTable.updatedAt] = DateTime.now()
            }
        }
    }

}



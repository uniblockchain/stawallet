package stacrypt.stawallet.bitcoin

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import stacrypt.stawallet.*
import stacrypt.stawallet.model.*
import java.lang.Exception
import java.util.logging.Level
import java.util.logging.Logger
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
) : BaseBlockchainWatcher {

    companion object {
        private val logger = Logger.getLogger("watcher")
    }

    val blockWatchGap get() = config.getLong("daemons.bitcoind.watcher.blockWatchGap")
    val mempoolWatchGap get() = config.getLong("daemons.bitcoind.watcher.mempoolWatchGap")

    val dispatcher: CoroutineDispatcher = newSingleThreadContext("$walletName-watcher")

    private var mempoolWatcherJob: Job? = null
    var blockWatcherJob: Job? = null

    private fun startMempoolWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {
            delay(mempoolWatchGap)
            // TODO Implement
//            (bitcoind.rpcClient.getMempoolDescendants() as List<Transaction>).forEach { tx ->
//                processOrphanTransaction(tx)
//            }
        }
    }

    private fun startBlockWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {

            try {
                // Find out witch block height should we sync with (if there is any unsynced block)
                val currentBestBlockHeight = bitcoind.rpcClient.getBlockCount()
                val walletDao = transaction { WalletDao.findById(walletName) }!!
                val latestSyncedHeight = walletDao.latestSyncedHeight
                // TODO: Store and compare `bestblockhash` and get back in case of incompatibility

                logger.log(Level.INFO, "$walletDao: Starting a new watching iteration...")
                logger.log(Level.INFO, "$walletDao: Latest Synced Height is: $latestSyncedHeight")
                logger.log(Level.INFO, "$walletDao: Current best block hash is: $currentBestBlockHeight")

                if (currentBestBlockHeight > latestSyncedHeight) {
                    // We have one or more new bocks to sync with
                    // TODO: Compare database with with `previousblockhash` of the following value
                    logger.log(
                        Level.WARNING,
                        "$walletDao: We are looking for block in height: ${latestSyncedHeight + 1}"
                    )

                    val blockToAnalyze = bitcoind.rpcClient.getBlockHash(latestSyncedHeight + 1)

                    logger.log(Level.INFO, "$walletDao: We are looking for block with hash: $blockToAnalyze")


                    val nextBlock = bitcoind.rpcClient.getBlockWithTransactions(blockToAnalyze)
                    nextBlock.tx?.forEach { tx ->
                        logger.log(Level.FINE, "$walletDao: We are looking at transaction: ${tx.txid}")
                        processTransaction(
                            nextBlock,
                            tx
                        )
                    }

                    transaction {
                        // Now it's time to update previous (unconfirmed) block's confirmations
                        for (i in 0..confirmationsRequires) {
                            val blockHash = bitcoind.rpcClient.getBlockHash(latestSyncedHeight - i)
                            val b = bitcoind.rpcClient.getBlock(blockHash)
                            b.tx?.forEach {
                                increaseConfirmations(
                                    analyzingBlockHash = blockToAnalyze,
                                    blockInfo = b,
                                    txHash = it
                                )
                            }
                        }

                        walletDao.latestSyncedHeight = latestSyncedHeight + 1
                    }

                    /**
                     * Try to move the extra amount of confirmed balance to the cold wallet
                     */
//                    transaction {
                    // TODO: Implement
//                        val confirmedBalance =
//                            UtxoTable.join(
//                                ProofTable, JoinType.INNER, UtxoTable.discoveryProof, null, null
//                            )
//                                .select { UtxoTable.wallet eq walletDao.id }
//                                .andWhere { UtxoTable.spendProof.isNull() }
//                                .andWhere { UtxoTable.isSpent eq false }
//                    }


                    // TODO: Fix and improve this
                    logger.log(Level.INFO, "$walletDao: Cleaning up the database")
//                    logger.log(
//                        Level.INFO, "$walletDao: Cleaned up " +
                    transaction {
                        exec(
                            """
                            delete from  proof
                                where (id not in (select proof from deposit where proof is not null))
                                and (id not in (select proof from task where proof is not null))
                                and (id not in (select discovery_proof from utxo where discovery_proof is not null))
                                and (id not in (select spend_proof from utxo where spend_proof is not null))
                            ;
                    """.trimIndent()
                        )
                    }
//                                .toString() + " unused proofs"
//                    )


                } else {
                    logger.log(Level.INFO, "$walletDao: There is nothing new, so we skip this iteration")
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Exception happened, we will come back again in the next iteration")
                e.printStackTrace()
            } finally {
                delay(blockWatchGap)
            }
        }
    }

    override fun startWatcher() {
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
     * Here we are searching for any associated address in our watching addresses for each incoming transaction utxo.
     */
    private fun findAssociatedAddress(vout: TransactionOutput): AddressDao? = transaction {
        // TODO: Review how we should recognize the addresses
        val address = vout.scriptPubKey?.addresses?.lastOrNull()
        if (vout.scriptPubKey?.type == "pubkeyhash" || address is String) {
            AddressTable
                .select { AddressTable.isActive eq true }
                .andWhere { AddressTable.wallet eq WalletDao[walletName].id }
                .andWhere { AddressTable.provision eq (address as String) }
                .lastOrNull()?.run { AddressDao.wrapRow(this) }
        } else {
            null
        }
    }

    /**
     * This method create a new proof for the related transaction, or return the existing one if it has been created
     * in advance.
     */
    private fun inquireProof(block: BlockInfoWithTransactions, tx: Transaction): ProofDao =
        transaction {
            val q = ProofTable.select { ProofTable.blockchain eq blockchainId }
                .andWhere { ProofTable.txHash eq tx.txid!! }
                .andWhere { ProofTable.blockHash eq block.hash!! }
                .andWhere { ProofTable.blockHeight eq block.height!!.toInt() }
                .firstOrNull()

            val proof: ProofDao
            proof = if (q == null)
            // Create a new proof for this
                ProofDao.new {
                    this.blockchain = WalletDao[walletName].blockchain
                    this.blockHash = block.hash
                    this.blockHeight = block.height!!.toInt()
                    this.txHash = tx.txid!!
                    this.confirmationsLeft = this@BitcoinBlockchainWatcher.confirmationsRequires
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
            if (UtxoTable.select { (UtxoTable.txid eq transaction.txid!!) and (UtxoTable.vout eq transactionOutput.n!!.toInt()) }.count() == 0) {
                // This is new UTXO!
                UtxoDao.new {
                    this.address = address
                    this.wallet = address.wallet
                    this.txid = transaction.txid!!
                    this.vout = transactionOutput.n!!.toInt()
                    this.amount = transactionOutput.value!!.btcToSat()
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
            logger.log(Level.INFO, "Error inquiring proof for tx: ${tx.txid}")
            e.printStackTrace()
        }
        proof as ProofDao

        tx.vout
            ?.asSequence()
            ?.map { vout ->
                val associatedAddress = findAssociatedAddress(vout)
                if (associatedAddress == null) logger.log(
                    Level.INFO,
                    "${tx.txid}: This transaction's vout: ${vout.n} doesn't have any associated pubkey address"
                )
                Pair(associatedAddress, vout)
            }
            ?.filter { it.first != null }
            ?.map {

                /**
                 * Insert new UTXOs
                 */
                logger.log(Level.INFO, "$walletName: New UTXO found!!!!!!")

                try {
                    insertNewUtxo(it.first!!, tx, it.second, proof)
                    logger.log(Level.INFO, "$walletName: New UTXO added!!!!!!")
                } catch (e: Exception) {

                    logger.log(Level.INFO, "$walletName: New UTXO adding error :(")
                    e.printStackTrace()
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
                logger.log(Level.INFO, "$walletName: New Deposit found!!!!!!")

                try {
                    insertNewDeposit(
                        relatedInvoice = v.firstOrNull()?.first,
                        transactionHash = tx.txid!!,
                        amount = v.sumByLong { it.second.value!!.btcToSat() },
                        proof = proof
                    )
                    logger.log(Level.INFO, "$walletName: New Deposit added!!!!!!")
                } catch (e: Exception) {
                    logger.log(Level.INFO, "$walletName: New Deposit adding error :(")
                    e.printStackTrace()
                    // TODO Report to the boss
                }

            }

    }

    private fun increaseConfirmations(analyzingBlockHash: String, blockInfo: BlockInfo, txHash: String) {
        ProofTable.update(
            {
                (ProofTable.blockchain eq blockchainId)
                    .and(ProofTable.txHash eq txHash)
                    .and(ProofTable.blockHash eq blockInfo.hash)
                    .and(ProofTable.blockHeight eq blockInfo.height!!.toInt())
                    .and(ProofTable.confirmationsLeft greater 0)
                    .and(ProofTable.confirmationsTrace.isNull() or ProofTable.confirmationsTrace.notLike("%$analyzingBlockHash%"))
            }
        ) {
            with(SqlExpressionBuilder) {
                it.update(ProofTable.confirmationsLeft, ProofTable.confirmationsLeft - 1)
                it.update(
                    ProofTable.confirmationsTrace,
                    ConcatWS(",", ProofTable.confirmationsTrace, stringParam(analyzingBlockHash))
                )
                it[ProofTable.updatedAt] = DateTime.now()
            }
        }
    }

}



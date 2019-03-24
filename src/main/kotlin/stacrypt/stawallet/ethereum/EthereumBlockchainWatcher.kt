package stacrypt.stawallet.ethereum

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.response.EthBlock
import stacrypt.stawallet.BaseBlockchainWatcher
import stacrypt.stawallet.config
import stacrypt.stawallet.model.*
import java.lang.Exception
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

class EthereumBlockchainWatcher(
    private val blockchainId: Int,
    private val walletName: String,
    private val confirmationsRequires: Int
) : BaseBlockchainWatcher {

    companion object {
        private val logger = Logger.getLogger("geth_watcher")
    }

    val dispatcher: CoroutineDispatcher = newSingleThreadContext("$walletName-watcher")

    val blockWatchGap get() = config.getLong("daemons.geth.watcher.blockWatchGap")
    var blockWatcherJob: Job? = null

    private fun startBlockWatcherJob(scope: CoroutineScope) = scope.launch {
        while (true) {

            try {
                // Find out witch block height should we sync with (if there is any unsynced block)
                val currentBestBlockHeight = geth.rpcClient.ethBlockNumber().send().blockNumber.toLong()
                val walletDao = transaction { WalletDao.findById(walletName) }!!
                val latestSyncedHeight = walletDao.latestSyncedHeight.toLong()
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

                    val nextBlock = geth.rpcClient.ethGetBlockByNumber(
                        DefaultBlockParameterNumber(latestSyncedHeight + 1), true
                    ).send().block
                    val blockToAnalyze = nextBlock.hash

                    logger.log(Level.INFO, "$walletDao: We are looking for block with hash: $blockToAnalyze")

                    nextBlock.transactions?.forEach { tx ->
                        tx as EthBlock.TransactionObject
                        logger.log(Level.FINE, "$walletDao: We are looking at transaction: ${tx.hash}")
                        processTransaction(
                            nextBlock,
                            tx
                        )
                    }

                    transaction {
                        // Now it's time to update previous (unconfirmed) block's confirmations
                        for (i in 0..confirmationsRequires) {
                            val block = geth.rpcClient.ethGetBlockByNumber(
                                DefaultBlockParameterNumber(latestSyncedHeight - i), false
                            ).send().block
                            block.transactions?.forEach {
                                it as EthBlock.TransactionHash
                                increaseConfirmations(
                                    analyzingBlockHash = blockToAnalyze,
                                    blockInfo = block,
                                    txHash = it.get()
                                )
                            }
                        }

                        walletDao.latestSyncedHeight = (latestSyncedHeight + 1).toInt()
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
                    logger.log(
                        Level.INFO,
                        "$walletDao: There is nothing new, so we skip this iteration"
                    )
                }
            } catch (e: Exception) {
                logger.log(
                    Level.SEVERE,
                    "Exception happened, we will come back again in the next iteration"
                )
                e.printStackTrace()
            } finally {
                delay(blockWatchGap)
            }
        }
    }

    private fun processTransaction(block: EthBlock.Block, tx: EthBlock.TransactionObject) {
        // FIXME: Handle Utxo's isSpent

        var proof: ProofDao? = null
        try {
            proof = inquireProof(block, tx)
            // TODO: Update proof
        } catch (e: Exception) {
            // TODO Report to the boss
            // Maybe invalid block info
            logger.log(Level.INFO, "Error inquiring proof for tx: ${tx.hash}")
            e.printStackTrace()
        }
        proof as ProofDao

        val associatedAddress = findAssociatedAddress(tx.to)
        if (associatedAddress == null) {
            logger.log(
                Level.INFO,
                "${tx.hash}: This transaction's receiver address: ${tx.to} doesn't have any associated address"
            )
        } else {
            val invoice = findRelatedInvoice(associatedAddress)
            if (invoice == null) {
                // It is a anonymous receiving amount. We could'nt proceed this deposit.
                // Just throw an exception to inform the admin
                // TODO Report to the boss
                logger.log(
                    Level.INFO,
                    "${tx.hash}: Could not found any related invoice to: ${tx.to}"
                )
            } else {
                /**
                 * Insert new Deposit
                 */
                logger.log(Level.INFO, "$walletName: New Deposit found!!!!!!")

                try {
                    // FIXME: Ensure it's real ethereum value, not token or s.th else...
                    insertNewDeposit(
                        relatedInvoice = invoice,
                        transactionHash = tx.hash,
                        amount = tx.value,
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

    }

    /**
     * This method create a new proof for the related transaction, or return the existing one if it has been created
     * in advance.
     */
    private fun inquireProof(block: EthBlock.Block?, tx: EthBlock.TransactionObject): ProofDao =
        transaction {
            val q = ProofTable.select { ProofTable.blockchain eq blockchainId }
                .andWhere { ProofTable.txHash eq tx.hash }
//                .andWhere { ProofTable.blockHash eq block.hash!! }
//                .andWhere { ProofTable.blockHeight eq block.height!!.toInt() }
                .firstOrNull()

            // FIXME: Think more:

            val proof: ProofDao
            proof = if (q == null)
            // Create a new proof for this
                ProofDao.new {
                    this.blockchain = WalletDao[walletName].blockchain
                    this.blockHash = block?.hash
                    this.blockHeight = block?.number?.toInt()
                    this.txHash = tx.hash!!
                    this.confirmationsLeft = this@EthereumBlockchainWatcher.confirmationsRequires
                }
            else
            // TODO: Check for validity
                ProofDao.wrapRow(q)

            if (proof.blockHash != block?.hash && proof.blockHeight != block?.number?.toInt()) {
                if (block?.hash == null || block?.number == null) {
                    // This is something unusual.
                    // TODO: Dangerous, report needed
                    // TODO: What should we do?
                } else {
                    // Update the proof block information
                    proof.blockHash = block.hash
                    proof.blockHeight = block.number?.toInt()
                    // And reset confirmations (not needed)
                    proof.confirmationsLeft = this@EthereumBlockchainWatcher.confirmationsRequires
                    proof.updatedAt = DateTime.now()
                }
            }

            proof
        }


    /**
     * Here we make a new deposit record, even when we are NOT SURE that it is a fully confirmed transaction.
     *
     * Note: The saved deposits are belongs to a specific invoice. If we couldn't find any related invoice, we will not
     * record this deposit.
     *
     */
    private fun insertNewDeposit(
        relatedInvoice: InvoiceDao,
        transactionHash: String,
        amount: BigInteger,
        feeAmount: BigInteger = 0.toBigInteger(),
        proof: ProofDao
    ) =
        transaction {
            // Find the related invoice
            when {
                DepositTable.leftJoin(ProofTable)
                    .select { DepositTable.invoice eq relatedInvoice.id }
                    .andWhere { ProofTable.txHash eq transactionHash }
                    .count() == 0 -> // This is new!
                    DepositDao.new {
                        this.grossAmount = amount.toLong() // FIXME FIXME FIXME
                        this.netAmount = max(0, (amount - feeAmount).toLong()) // FIXME FIXME FIXME
                        this.invoice = relatedInvoice
                        this.proof = proof
                    }
                else -> {
                    // We are reading a blockchain again. This is dangerous!!!
                    // TODO Report to the boss
                }
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
     * Here we are searching for any associated address.
     */
    private fun findAssociatedAddress(address: String?): AddressDao? = transaction {
        if (address != null) {
            AddressTable
                .select { AddressTable.isActive eq true }
                .andWhere { AddressTable.wallet eq WalletDao[walletName].id }
                .andWhere { AddressTable.provision eq address }
                .lastOrNull()?.run { AddressDao.wrapRow(this) }
        } else {
            null
        }
    }


    override fun startWatcher() {
        GlobalScope.launch(this.dispatcher) {
            blockWatcherJob = startBlockWatcherJob(this)
        }

    }

    suspend fun stopWatcherAndJoin() {
        if (blockWatcherJob?.isActive == true) blockWatcherJob!!.cancelAndJoin()
    }

    fun stopWatcher() {
        if (blockWatcherJob?.isActive == true) blockWatcherJob!!.cancel()
    }

    private fun increaseConfirmations(analyzingBlockHash: String, blockInfo: EthBlock.Block, txHash: String) {
        ProofTable.update(
            {
                (ProofTable.blockchain eq blockchainId)
                    .and(ProofTable.txHash eq txHash)
                    .and(ProofTable.blockHash.isNotNull())
                    .and(ProofTable.blockHeight.isNotNull())
                    .and(ProofTable.blockHash eq blockInfo.hash)
                    .and(ProofTable.blockHeight eq blockInfo.number!!.toInt())
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
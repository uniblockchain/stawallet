package stacrypt.stawallet.bitcoin

import com.typesafe.config.Config
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.kethereum.encodings.encodeToBase58WithChecksum
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toMinimalByteArray
import org.kethereum.hashes.sha256
import org.kethereum.ripemd160.calculateRIPEMD160
import org.walleth.khex.hexToByteArray
import stacrypt.stawallet.*
import stacrypt.stawallet.model.*
import java.math.BigDecimal
import java.util.logging.Logger
import kotlin.math.roundToLong
import org.walleth.khex.toHexString
import stacrypt.stawallet.SecretProvider.Companion.MAGIC_NUMBER
import java.math.BigInteger


const val NETWORK_MAINNET = "mainnet"
const val NETWORK_TESTNET_3 = "testnet3"

private val logger = Logger.getLogger("wallet")

data class InvalidBitcoinAddressException(val wallet: String, val address: String?) :
    Exception("wallet $wallet invalid address: $address")

class BitcoinWallet(
    name: String,
    network: String,
    override val requiredConfirmations: Int,
    secretProvider: SecretProvider
) :
    Wallet(name, secretProvider, network) {
    override fun validateAddress(address: String): Boolean = bitcoind.rpcClient.validateAddress(address).isvalid


    override fun blockchainExplorerTxLink(txId: String) = "https://www.blockchain.com/btc/tx/$txId"

    override suspend fun lastUsableInvoice(user: String): InvoiceDao? =
        InvoiceTable.innerJoin(AddressTable).select {
            (InvoiceTable.wallet eq name) and (InvoiceTable.user eq user) and (AddressTable.isActive eq true) and (InvoiceTable.expiration.isNull() or (InvoiceTable.expiration greater DateTime.now()))
        }.lastOrNull()?.run { InvoiceDao.wrapRow(this) }

    private fun newAddress(isChange: Boolean = false): AddressDao {
        val q = AddressTable.select { AddressTable.wallet eq name }.orderBy(AddressTable.id, false).firstOrNull()
        var newIndex = 0
        if (q != null) {
            val lastIssuedAddress = AddressDao.wrapRow(q)
            newIndex = lastIssuedAddress.path.split("/").last().toInt() + 1
        }

        val newPath = secretProvider.makePath(newIndex, if (isChange) 1 else 0)

        val newPublicKey = secretProvider.getHotPublicKey(newPath)
        return AddressDao.new {
            this.wallet = WalletDao.findById(name)!!
            this.publicKey = newPublicKey
            this.provision = formatP2pkh(newPublicKey)
            this.path = newPath
        }
    }

    override suspend fun issueNewInvoice(user: String): InvoiceDao {
        return InvoiceDao.new {
            this.wallet = WalletDao.findById(name)!!
            this.address = newAddress(false)
            this.user = user
        }
    }

    private fun formatP2pkh(publicKey: ByteArray) =
        publicKey.toBitcoinAddress(
            if (this@BitcoinWallet.network == NETWORK_MAINNET) VERSION_BYTE_P2PKH_MAINNET else VERSION_BYTE_P2PKH_TESTNET3
        )

    companion object {
        const val TX_BASE_SIZE = 10 // Bytes
        const val TX_INPUT_SIZE = 148 // Bytes
        const val TX_OUTPUT_SIZE = 34 // Bytes

        const val CRYPTOCURRENCY = "BTC"

        fun coinType(network: String) = if (network == NETWORK_MAINNET) 0 else 1
    }

    override val daemon = bitcoind

    override fun startBlockchainWatcher(): BaseBlockchainWatcher {
        blockchainWatcher = bitcoind.addBlockchainWatcher(
            transaction { WalletDao[name].blockchain.id.value },
            name,
            this.requiredConfirmations
        )
        return blockchainWatcher!!
    }

    override fun stopBlockchainWatcher() =
        bitcoind.removeBlockchainWatcher((blockchainWatcher as BitcoinBlockchainWatcher?)!!)

    init {
//        rpcClient.getMemoryInfo()
//        rpcClient.getBlockchainInfo()
//        rpcClient.getMempoolInfo()
//        rpcClient.getNetworkInfo()
//        rpcClient.getMiningInfo()
//        rpcClient.getPeerInfo()
//        rpcClient.getAddedNodeInfo()
//        rpcClient.getUnspentTransactionOutputSetInfo()
//        rpcClient.estimateSmartFee(1)
//        rpcClient.importMultipleAddresses(arrayListOf(stacrypt.stawallet.bitcoin.AddressOrScript(object {
//            val address = "mxpejj3Wf2kvaiRUgz9CkWYwQx3HkxhiLf"
//        }, 1543599566)), stacrypt.stawallet.bitcoin.ImportAddressOptions(true))
    }

    override suspend fun syncBlockchain() {
        // Load all to watch addresses(if any)

//        db.computeInReadonlyTransaction { store.get(it, StringBinding.stringToEntry("")) }


        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun selectUtxo(amountToSend: Long, baseFee: Long, feePerExtraUtxo: Long): List<UtxoDao> {
        var estimatedFee = baseFee
        var totalInputAmount = 0L

        val utxos = UtxoDao.wrapRows(
            UtxoTable.leftJoin(ProofTable, { UtxoTable.discoveryProof }, { ProofTable.id })
                .select { UtxoTable.wallet eq name }
                .andWhere { UtxoTable.isSpent eq false }
                .andWhere { UtxoTable.spendProof.isNull() }
                .andWhere { ProofTable.confirmationsLeft eq 0 }
                .orderBy(UtxoTable.amount, false)
        ).filter {
            when {
                totalInputAmount >= amountToSend + estimatedFee -> false
                else -> {
                    totalInputAmount += it.amount
                    estimatedFee += feePerExtraUtxo
                    true
                }
            }
        }

        if (totalInputAmount >= amountToSend + estimatedFee) return utxos
        else throw NotEnoughFundException(name, amountToSend)
    }

    override suspend fun sendTo(address: String, amountToSend: BigInteger, tag: Any?): String =
    // FIXME: IMPORTANT!!! Check the value unit -> sat or btc?
        transaction {
            val outputs = mutableMapOf(address to BigDecimal(amountToSend))

            val satPerByte = daemon.fairTxFeeRate()!!

            val utxos = selectUtxo(
                amountToSend.toLong(),
                (TX_BASE_SIZE + TX_OUTPUT_SIZE * 2) * satPerByte,
                TX_INPUT_SIZE * satPerByte
            )

            val amountToChange = utxos.sumByLong { it.amount } -
                    amountToSend.toLong() -
                    (TX_BASE_SIZE + TX_OUTPUT_SIZE * 2) * satPerByte -
                    utxos.size * TX_INPUT_SIZE * satPerByte
            if (amountToChange > 0) {
                outputs[newAddress(true).provision] = amountToChange.toBigDecimal()
            }

            var transaction = daemon.rpcClient.createRawTransaction(
                inputs = utxos.map { OutPoint(it.txid, it.vout) },
                outputs = outputs
            ).hexToByteArray()
            // TODO: Validate the raw transaction (because we do NOT trust the bitcoind)

            val signatures = utxos.map {
                secretProvider.getHotPrivateKey(it.address.path).toBitcoinWif(
                    if (this@BitcoinWallet.network == NETWORK_MAINNET) WIF_PREFIX_MAINNET else WIF_PREFIX_TESTNET
                )
            }

            val signingResult = daemon.rpcClient.signRawTransactionWithKey(transaction.toHexString(""), signatures)
            if (signingResult.complete == true && signingResult.errors.isNullOrEmpty() && signingResult.hex != null) {
                transaction = signingResult.hex.hexToByteArray()
            } else {
                // TODO: Handle error
            }

            val txHash = daemon.rpcClient.sendRawTransaction(transaction.toHexString(""))

            utxos.forEach { it.isSpent = true }

            txHash
        }

    override fun initializeToDb(force: Boolean): WalletDao {
        var walletDao = WalletDao.findById(name)

        if (walletDao != null && force) {
            walletDao.delete()
            walletDao = null
        }

        if (walletDao == null) {
            walletDao = WalletDao.new(name) {
                blockchain = BlockchainTable.select { BlockchainTable.currency eq CRYPTOCURRENCY }
                    .andWhere { BlockchainTable.network eq this@BitcoinWallet.network }
                    .firstOrNull()
                    ?.run { BlockchainDao.wrapRow(this) }
                    ?: BlockchainDao.new {
                        this.currency = CRYPTOCURRENCY
                        this.network = this@BitcoinWallet.network
                    }

                seedFingerprint = "" // FIXME
                path = "m/$MAGIC_NUMBER'/${secretProvider.coinType}'/${secretProvider.accountId}'"
                latestSyncedHeight = 1_485_508 // FIXME
            }
        }

        return walletDao
    }

}

const val VERSION_BYTE_P2PKH_MAINNET = 0
const val VERSION_BYTE_P2PKH_TESTNET3 = 111

/**
 * Resources:
 *  * https://en.bitcoin.it/wiki/List_of_address_prefixes
 *  * https://en.bitcoin.it/wiki/Technical_background_of_version_1_Bitcoin_addresses
 */
fun ByteArray.toBitcoinAddress(versionByte: Int) = this
    .getCompressedPublicKey()
    .sha256()
    .calculateRIPEMD160()
    .run { versionByte.toMinimalByteArray() + this }
    .encodeToBase58WithChecksum()

/**
 * Resources:
 *  * https://en.bitcoin.it/wiki/Wallet_import_format
 */
const val WIF_PREFIX_MAINNET = 0x80
const val WIF_PREFIX_TESTNET = 0xef

fun ByteArray.toBitcoinWif(networkByte: Int) = this
    .apply { assert(this.size == 32) } // Check padding exclusion
    .run { networkByte.toMinimalByteArray() + this + 0x01.toMinimalByteArray() } // Compression byte
    .encodeToBase58WithChecksum()

fun BigInteger.toBitcoinWif(networkByte: Int) = toBytesPadded(32).toBitcoinWif(networkByte)

fun Double.btcToSat() = (this * 100_000_000.0).roundToLong()
fun BigDecimal.btcToSat() = scaleByPowerOfTen(8).toLong()

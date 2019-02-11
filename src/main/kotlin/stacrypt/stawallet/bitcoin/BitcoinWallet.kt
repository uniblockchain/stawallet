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
import java.math.BigInteger


const val NETWORK_MAINNET = "mainnet"
const val NETWORK_TESTNET_3 = "testnet3"

private val logger = Logger.getLogger("wallet")

data class NotEnoughFundException(val wallet: String, val amountToPay: Long = 0L) :
    Exception("wallet $wallet does NOT have enough money to pay $amountToPay")

data class InvalidBitcoinAddressException(val wallet: String, val address: String?) :
    Exception("wallet $wallet invalid address: $address")

class BitcoinWallet(name: String, config: Config, network: String) :
    Wallet(name, ConfigSecretProvider(config, if (network == NETWORK_MAINNET) 0 else 1), network) {

    override var latestBlockHeight = 0
    override var requiredConfirmations = config.getInt("requiredConfirmations")
//        if (config.hasPath("requiredConfirmations")) config.getInt("requiredConfirmations") else null

    override fun blockchainExplorerTxLink(txid: String) = "https://www.blockchain.com/btc/tx/$txid"

    override suspend fun invoiceDeposits(invoiceId: Int): List<DepositDao> =
        DepositDao.wrapRows(
            DepositTable.select { DepositTable.invoice eq InvoiceDao[invoiceId].id }.orderBy(DepositTable.id, false)
        ).toList()

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
    }

    override val daemon = bitcoind

    fun startBlockchainWatcher() =
        bitcoind.addBlockchainWatcher(
            transaction { WalletDao[name].blockchain.id.value },
            name,
            this.requiredConfirmations
        )

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

    override suspend fun sendTo(address: String, amountToSend: Long) {
        transaction {
            val outputs = mutableMapOf(address to BigDecimal(amountToSend))

            val satPerByte = daemon.fairTxFeeRate()!!

            val utxos = selectUtxo(
                amountToSend,
                (TX_BASE_SIZE + TX_OUTPUT_SIZE * 2) * satPerByte,
                TX_INPUT_SIZE * satPerByte
            )

            val amountToChange = utxos.sumByLong { it.amount } -
                    amountToSend -
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
                secretProvider.signTxWithHotPrivateKey(transaction, it.address.path)
            }

//            var totalSigSize = 0
//            signatures.forEachIndexed { index, s ->
//                val pos = 4 + 1 + totalSigSize + ()*index + 32 + 4
//                transaction = transaction[] + ""
//                totalSigSize += 1 + s.hexToByteArray().size
//            }

//            for (i in 0 until transaction.getInputs().size()) {
//
//                val transactionInput = transaction.getInput(i)
//                val addressFromUtxo = mUTXOs.get(i).getAddress()
//                val privKeyBytes = getPrivKeyBitesForAddress(addressFromUtxo)
//                val ecKey = ECKey.fromPrivate(privKeyBytes)
//
//                val scriptPubKey =
//                    ScriptBuilder.createOutputScript(Address.fromBase58(params, mUTXOs.get(i).getAddress()))
//
//                val hash = transaction.hashForSignature(i, scriptPubKey, Transaction.SigHash.ALL, false)
//                val ecSig = ecKey.sign(hash)
//                val txSig = TransactionSignature(ecSig, Transaction.SigHash.ALL, false)
//                transactionInput.setScriptSig(ScriptBuilder.createInputScript(txSig, ecKey))
//            }
//
////serialization and broadcasting
//            val bytesRawTransaction = transaction.bitcoinSerialize()
//            val rawTransaction = HEX.encode(bytesRawTransaction)
//            broadcastTx(rawTransaction)


            // TODO: Sign the transaction
            // TODO: Push to blockchain
            daemon.rpcClient.signRawTransaction(transaction.toHexString())
            daemon.rpcClient.sendRawTransaction(transaction.toHexString())

            utxos.forEach { it.isSpent = true }

        }
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

package stacrypt.stawallet.bitcoin

import com.typesafe.config.Config
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime
import org.kethereum.encodings.encodeToBase58WithChecksum
import org.kethereum.extensions.toMinimalByteArray
import org.kethereum.hashes.sha256
import org.kethereum.ripemd160.calculateRIPEMD160
import stacrypt.stawallet.*
import stacrypt.stawallet.model.*
import java.util.logging.Logger
import kotlin.math.roundToLong


const val NETWORK_MAINNET = "mainnet"
const val NETWORK_TESTNET_3 = "testnet3"

private val logger = Logger.getLogger("wallet")

data class NotEnoughFundException(val wallet: String, val amountToPay: Long = 0L) :
    Exception("wallet $wallet does NOT have enough money to pay $amountToPay")

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

    override suspend fun issueNewInvoice(user: String): InvoiceDao {
        val q = AddressTable.select { AddressTable.wallet eq name }.orderBy(AddressTable.id, false).firstOrNull()
        var newIndex = 0
        if (q != null) {
            val lastIssuedAddress = AddressDao.wrapRow(q)
            newIndex = lastIssuedAddress.path.split("/").last().toInt() + 1
        }

        val newPath = secretProvider.makePath(newIndex, 0)

        val newPublicKey = secretProvider.getHotPublicKey(newPath)
        val newAddress = AddressDao.new {
            this.wallet = WalletDao.findById(name)!!
            this.publicKey = newPublicKey
            this.provision = formatP2pkh(newPublicKey)
            this.path = newPath
        }

        return InvoiceDao.new {
            this.wallet = WalletDao.findById(name)!!
            this.address = newAddress
            this.user = user
        }
    }

    private fun formatP2pkh(publicKey: ByteArray) =
        publicKey.toBitcoinAddressBinary(
            if (this@BitcoinWallet.network == NETWORK_MAINNET) VERSION_BYTE_P2PKH_MAINNET else VERSION_BYTE_P2PKH_TESTNET3
        )

    companion object {
        const val TX_BASE_SIZE = 10 // Bytes
        const val TX_INPUT_SIZE = 148 // Bytes
        const val TX_OUTPUT_SIZE = 34 // Bytes
    }

    override val daemon = bitcoind
//    override val storage = UtxoStorage(name)

//    var balance: Long
//        set(_) = throw Exception("You can not change the balance manually!")
//        get() = store.computeInReadonlyTransaction { tx ->
//            tx.getAll(STORE_TYPE_UTXO).sumByLong { it.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long }
//        }


    // Db:
//    private val store = PersistentEntityStores.newInstance("${config.getString("db.envPath")}/$wallet")!!

//    /**
//     * `btc:info` is a key-value store. Here is the usage:
//     *
//     * lastUpdateTime
//     * lastIssuedIndex
//     * lastBlockHeight
//     * hotBalance
//     */
//    val infoStore = db.computeInTransaction { db.openStore("$wallet:info", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:watch` is a hashMap: address ->
//     */
//    val watchStore = db.computeInTransaction { db.openStore("$wallet:addr:watch", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:watch` is a hashMap: address -> {index, time, }
//     */
//    val archiveStore = db.computeInTransaction { db.openStore("$wallet:addr:all", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:utxo` is
//     */
//    val utxoStore = db.computeInTransaction { db.openStore("$wallet:utxo", StoreConfig.WITHOUT_DUPLICATES, it) }

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


    override suspend fun sendTo(address: String, amountToSend: Long) {
    }
//    override suspend fun sendTo(address: String, amountToSend: Long) {
//        val outputs = mapOf(address to BigDecimal(amountToSend))
//
//        val satPerByte = daemon.fairTxFeeRate()!!
//        storage.watch {
//            val utxos =
//                selectUtxo(amountToSend, (TX_BASE_SIZE + TX_OUTPUT_SIZE * 2) * satPerByte, TX_INPUT_SIZE * satPerByte)
//            val inputs = utxos.map {
//                OutPoint(it.first.split(":")[0], it.first.split(":")[1].toInt())
//            }.toList()
//
//            val amountToChange = utxos.sumByLong { it.second } -
//                    amountToSend -
//                    (TX_BASE_SIZE + TX_OUTPUT_SIZE * 2) * satPerByte -
//                    utxos.size * TX_INPUT_SIZE * satPerByte
//            if (amountToChange > 0) {
//                val newChangeAddress = secretProvider.getHotAddress(nextChangeAddressIndex().toInt(), 1)
//                outputs.plus(Pair(newChangeAddress, amountToChange))
//            }
//
//            removeUtxo(*inputs.map { "${it.txid}:${it.vout}" }.toTypedArray())
//            var transaction = daemon.rpcClient.createRawTransaction(inputs = inputs, outputs = outputs)
//
//
//        }

//        storage.jedis.watch("$name:")
//
//        val outputs = mapOf(address to BigDecimal(amountToSend))
//        val inputs = ArrayList<OutPoint>()
//
//        var estimatedFee = BASE_FEE * feeRate
//        var inputAmount = 0L
//
//
//        // Iterate UTXOs to make transaction:
//        storage.jedis.zscan("", "$name:$KEY_UTXO").result.forEach {
//            inputs.add(
//                OutPoint(
//                    utxo.getProperty(STORE_TYPE_UTXO_TXHASH).toString(),
//                    utxo.getProperty(STORE_TYPE_UTXO_VOUT) as Int
//                )
//            )
//            estimatedFee += FEE_PER_EXTRA_INPUT * feeRate
//            inputAmount += utxo.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long
//            if (inputAmount >= amountToSend + estimatedFee) break
//        }
//

//    override suspend fun sendTo(address: String, amountToSend: Long) {
//        store.computeInExclusiveTransaction {
//            val feeRate = 1 // TODO
//
//            var estimatedFee = BASE_FEE * feeRate
//            var inputAmount = 0L
//
//            val outputs = mapOf(address to BigDecimal(amountToSend))
//            val inputs = ArrayList<OutPoint>()
//
//            for (utxo in it.sort(STORE_TYPE_UTXO, STORE_TYPE_UTXO_AMOUNT, false)) {
//                inputs.add(
//                    OutPoint(
//                        utxo.getProperty(STORE_TYPE_UTXO_TXHASH).toString(),
//                        utxo.getProperty(STORE_TYPE_UTXO_VOUT) as Int
//                    )
//                )
//                estimatedFee += FEE_PER_EXTRA_INPUT * feeRate
//                inputAmount += utxo.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long
//                if (inputAmount >= amountToSend + estimatedFee) break
//            }
//
//            if (inputAmount < amountToSend + estimatedFee) throw stacrypt.stawallet.bitcoin.NotEnoughFundException(wallet, amountToSend)
//
//            if (inputAmount > amountToSend + estimatedFee) outputs.plus(
//                Pair(
////                    generateChangeAddress,
//                    null,
//                    inputAmount - amountToSend - estimatedFee
//                )
//            )
//
//            var transaction = rpcClient.createRawTransaction(inputs = inputs, outputs = outputs)
//            //TODO sign
//            rpcClient.sendRawTransaction(transaction)
//
//        }
//
//
//
//
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

}

const val VERSION_BYTE_P2PKH_MAINNET = 0
const val VERSION_BYTE_P2PKH_TESTNET3 = 111

/**
 * Resources:
 *  * https://en.bitcoin.it/wiki/List_of_address_prefixes
 *  * https://en.bitcoin.it/wiki/Technical_background_of_version_1_Bitcoin_addresses
 */
fun ByteArray.toBitcoinAddressBinary(versionByte: Int) = this
    .getCompressedPublicKey()
    .sha256()
    .calculateRIPEMD160()
    .run { versionByte.toMinimalByteArray() + this }
    .encodeToBase58WithChecksum()

fun Double.btcToSat() = (this * 100_000_000.0).roundToLong()

package stacrypt.stawallet.bitcoin

import com.typesafe.config.Config
import jetbrains.exodus.entitystore.PersistentEntityStores
import stacrypt.stawallet.*
import stacrypt.stawallet.UtxoStorage.Companion.STORE_TYPE_UTXO
import stacrypt.stawallet.UtxoStorage.Companion.STORE_TYPE_UTXO_AMOUNT
import java.util.logging.Logger


private val logger = Logger.getLogger("wallet")

data class NotEnoughFundException(val coin: String, val amountToPay: Long = 0L) :
    Exception("wallet $coin does NOT have enough money to pay $amountToPay")


class BitcoinWallet(name: String, config: Config) : Wallet(name, daemon, ConfigSecretProvider(config, 0)) {
    companion object {
        val daemon = object : WalletDaemon() {
            override var status: DaemonState
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
                set(value) {}

            override fun createRpcClient(): BitcoinRpcClient = BitcoinRpcClient.createNewWithDefaultConfig()

        }

        private const val KEY_UTXO = "utxo"
        private const val KEY_TX = "tx"
        private const val KEY_TXID = "txid"
        private const val KEY_ADDR = "addr"

    }

    override val storage = UtxoStorage(name)

    private val BASE_FEE = 100L
    private val FEE_PER_EXTRA_INPUT = 10L

    override val coin = "btc"
    val rpcClient = (daemon.createRpcClient() as BitcoinRpcClient).commander

//    var balance: Long
//        set(_) = throw Exception("You can not change the balance manually!")
//        get() = store.computeInReadonlyTransaction { tx ->
//            tx.getAll(STORE_TYPE_UTXO).sumByLong { it.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long }
//        }


    // Db:
//    private val store = PersistentEntityStores.newInstance("${config.getString("db.envPath")}/$coin")!!

//    /**
//     * `btc:info` is a key-value store. Here is the usage:
//     *
//     * lastUpdateTime
//     * lastIssuedIndex
//     * lastBlockHeight
//     * hotBalance
//     */
//    val infoStore = db.computeInTransaction { db.openStore("$coin:info", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:watch` is a hashMap: address ->
//     */
//    val watchStore = db.computeInTransaction { db.openStore("$coin:addr:watch", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:watch` is a hashMap: address -> {index, time, }
//     */
//    val archiveStore = db.computeInTransaction { db.openStore("$coin:addr:all", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:utxo` is
//     */
//    val utxoStore = db.computeInTransaction { db.openStore("$coin:utxo", StoreConfig.WITHOUT_DUPLICATES, it) }

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
    }

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
//            if (inputAmount < amountToSend + estimatedFee) throw stacrypt.stawallet.bitcoin.NotEnoughFundException(coin, amountToSend)
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

//    suspend fun estimateFee(): Long {
//        rpcClient.est
//    }

}

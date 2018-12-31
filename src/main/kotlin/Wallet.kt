import jetbrains.exodus.entitystore.EntityId
import jetbrains.exodus.entitystore.PersistentEntityStores
import jetbrains.exodus.env.StoreConfig
import java.math.BigDecimal
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger("Wallet")

data class NotEnoughFundException(val coin: String, val amountToPay: Long = 0L) :
    Exception("Wallet $coin does NOT have enough money to pay $amountToPay")

class BitcoinWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {

    private val BASE_FEE = 100L
    private val FEE_PER_EXTRA_INPUT = 10L

    private val STORE_TYPE_UTXO = "utxo"
    private val STORE_TYPE_UTXO_TXHASH = "txhash"
    private val STORE_TYPE_UTXO_AMOUNT = "utxo"
    private val STORE_TYPE_UTXO_VOUT = "vout"

    override val coin = "btc"
    override val rpcClient = RpcClientFactory.createBitcoinClient(
        user = config.getString("wallet.$coin.rpc.username"),
        password = config.getString("wallet.$coin.rpc.password"),
        host = config.getString("wallet.$coin.rpc.host"),
        port = config.getInt("wallet.$coin.rpc.port"),
        secure = config.getBoolean("wallet.$coin.rpc.secure")
    )

    override var balance: Long
        set(_) = throw Exception("You can not change the balance manually!")
        get() = store.computeInReadonlyTransaction { tx ->
            tx.getAll(STORE_TYPE_UTXO).sumByLong { it.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long }
        }

    // Db:
    private val store = PersistentEntityStores.newInstance("${config.getString("db.envPath")}/$coin")!!

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
//        rpcClient.importMultipleAddresses(arrayListOf(AddressOrScript(object {
//            val address = "mxpejj3Wf2kvaiRUgz9CkWYwQx3HkxhiLf"
//        }, 1543599566)), ImportAddressOptions(true))
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
        store.computeInExclusiveTransaction {
            val feeRate = 1 // TODO

            var estimatedFee = BASE_FEE * feeRate
            var inputAmount = 0L

            val outputs = mapOf(address to BigDecimal(amountToSend))
            val inputs = ArrayList<OutPoint>()

            for (utxo in it.sort(STORE_TYPE_UTXO, STORE_TYPE_UTXO_AMOUNT, false)) {
                inputs.add(
                    OutPoint(
                        utxo.getProperty(STORE_TYPE_UTXO_TXHASH).toString(),
                        utxo.getProperty(STORE_TYPE_UTXO_VOUT) as Int
                    )
                )
                estimatedFee += FEE_PER_EXTRA_INPUT * feeRate
                inputAmount += utxo.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long
                if (inputAmount >= amountToSend + estimatedFee) break
            }

            if (inputAmount < amountToSend + estimatedFee) throw NotEnoughFundException(coin, amountToSend)

            if (inputAmount > amountToSend + estimatedFee) outputs.plus(
                Pair(
                    generateChangeAddress,
                    inputAmount - amountToSend - estimatedFee
                )
            )

            var transaction = rpcClient.createRawTransaction(inputs = inputs, outputs = outputs)
            //TODO sign
            rpcClient.sendRawTransaction(transaction)

        }




        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    suspend fun estimateFee(): Long {
//        rpcClient.est
//    }

}

//class LitecoinWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {
////    override val rpcClient: RpcClient = RpcClient()
//
//    override suspend fun syncBlockchain() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun subscribe() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun sendTo(address: String, amount: Long) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//}
//
//class EthereumWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {
////    override val rpcClient: RpcClient = RpcClient()
//
//    override suspend fun syncBlockchain() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun subscribe() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun sendTo(address: String, amount: Long) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//}
//
//class RippleWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {
////    override val rpcClient: RpcClient = RpcClient()
//
//    override suspend fun syncBlockchain() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun subscribe() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun sendTo(address: String, amount: Long) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//}


abstract class CryptocurrencyWalletEvent() {}

abstract class Wallet(coldAddress: String, hotXPrv: String) {

    abstract val coin: String
    var isSynced = false
    abstract val rpcClient: Any

    abstract var balance: Long

    companion object {
        val all = ArrayList<Wallet>()

        fun init() {
            for (wc in config.getObject("wallet").toList()) {

                val xPrv = config.getString("wallet.${wc.first}.hotXPrv")
                val address = config.getString("wallet.${wc.first}.coldAddress")

                when (wc.first) {
                    "btc" -> all.add(BitcoinWallet(address, xPrv))
//                    "ltc" -> all.add(LitecoinWallet(address, xPrv))
//                    "eth" -> all.add(EthereumWallet(address, xPrv))
//                    "xrp" -> all.add(RippleWallet(address, xPrv))
//                    else -> throw RuntimeException("Unsupported coin ${wc.first}!!!")
                }

                logger.log(Level.INFO, "Wallet found: ${wc.first}")
            }

        }
    }


    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit
    abstract suspend fun sendTo(address: String, amount: Long): Unit
}


//class BitcoinWallet() : BitcoinWallet() {
//    override fun provision(): String {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    constructor(address: String) : super(address)
//    constructor(xprv: String, index: Int, change: Int = 0) : super("")
//
//
//}


//class Wallet(val code: String) {
//    constructor()
//    abstract makeTransaction()
//    abstract fun address(): String
//
//
//    object rpc {
//
//    }
//}
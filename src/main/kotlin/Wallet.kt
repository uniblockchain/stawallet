import java.lang.RuntimeException
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger("Wallet")

class BitcoinWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {

    val rpcClient = RpcClientFactory.createBitcoinClient(
        user = config.getString("wallet.btc.rpc.username"),
        password = config.getString("wallet.btc.rpc.password"),
        host = config.getString("wallet.btc.rpc.host"),
        port = config.getInt("wallet.btc.rpc.port"),
        secure = config.getBoolean("wallet.btc.rpc.secure")
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
        rpcClient.importMultipleAddresses(arrayListOf(AddressOrScript(object {
            val address = "mxpejj3Wf2kvaiRUgz9CkWYwQx3HkxhiLf"
        }, 1543599566)), ImportAddressOptions(true))
    }

    override suspend fun syncBlockchain() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun sendTo(address: String, amount: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    suspend fun estimateFee(): Long {
//        rpcClient.est
//    }

}

class LitecoinWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {
//    override val rpcClient: RpcClient = RpcClient()

    override suspend fun syncBlockchain() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun sendTo(address: String, amount: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class EthereumWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {
//    override val rpcClient: RpcClient = RpcClient()

    override suspend fun syncBlockchain() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun sendTo(address: String, amount: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class RippleWallet(coldAddress: String, hotXPrv: String) : Wallet(coldAddress, hotXPrv) {
//    override val rpcClient: RpcClient = RpcClient()

    override suspend fun syncBlockchain() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun sendTo(address: String, amount: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}


abstract class CryptocurrencyWalletEvent() {}

abstract class Wallet(coldAddress: String, hotXPrv: String) {

    var isSynced = false
//    abstract val rpcClient: RpcClient

    companion object {
        val all = ArrayList<Wallet>()

        fun init() {
            for (wc in config.getObject("wallet").toList()) {

                val xPrv = config.getString("wallet.${wc.first}.hotXPrv")
                val address = config.getString("wallet.${wc.first}.coldAddress")

                when (wc.first) {
                    "btc" -> all.add(BitcoinWallet(address, xPrv))
                    "ltc" -> all.add(LitecoinWallet(address, xPrv))
                    "eth" -> all.add(EthereumWallet(address, xPrv))
                    "xrp" -> all.add(RippleWallet(address, xPrv))
                    else -> throw RuntimeException("Unsupported coin ${wc.first}!!!")
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
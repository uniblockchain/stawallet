import java.lang.RuntimeException

abstract class CryptocurrencyWalletEvent() {}

abstract class Wallet {

    companion object {
        val all = ArrayList<Wallet>()

        fun init() {
            for (wc in config.getConfig("wallet").entrySet()) {
                val w: Wallet
                when (wc.key) {
                    "btc" -> w = object : Wallet() {
                        override suspend fun syncBlockchain() {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override suspend fun subscribe() {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                    }
                    else -> throw RuntimeException("Unsupported coin ${wc.key}!!!")

                }
                all.add(w)
            }
        }

    }

    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit
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
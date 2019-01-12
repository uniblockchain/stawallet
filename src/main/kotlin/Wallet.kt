import bitcoin.BitcoinWallet
import java.util.logging.Level

abstract class Wallet(val name: String) {

    init {
        val xPrv = config.getString("wallets.${name}.seed")
        val address = config.getString("wallets.${name}.coldAddress")
    }

    private val BITCOIN_SEED = "Bitcoin seed".toByteArray()

    abstract val coin: String
    var isSynced = false
    abstract val rpcClient: Any

    abstract var balance: Long

    companion object {
        val all = ArrayList<Wallet>()

        fun init() {
            for (wc in config.getObject("wallet").toList()) {

                when (config.getString("wallets.${wc.first}.cryptocurrency")) {
                    "bitcoin" -> all.add(BitcoinWallet(wc.first))
//                    "litecoin" -> all.add(LitecoinWallet(address, xPrv))
//                    "ethereum" -> all.add(EthereumWallet(address, xPrv))
//                    "ripple" -> all.add(RippleWallet(address, xPrv))
//                    else -> throw RuntimeException("Unsupported coin ${wc.first}!!!")
                }

                wallet.logger.log(Level.INFO, "Wallet found: ${wc.first}")
            }

        }
    }

    fun derive(seed: ByteArray) {
//        try {
//            val mac = Mac.getInstance("HmacSHA512", "BC")
//            val seedkey = SecretKeySpec(BITCOIN_SEED, "HmacSHA512")
//            mac.init(seedkey)
//            val lr = mac.doFinal(seed)
//            val l = Arrays.copyOfRange(lr, 0, 32)
//            val r = Arrays.copyOfRange(lr, 32, 64)
//            val m = BigInteger(1, l)
//            if (m.compareTo(curve.getN()) >= 0) {
//                throw ValidationException("This is rather unlikely, but it did just happen")
//            }
//            val keyPair = ECKeyPair(l, true)
//            return ExtendedKey(keyPair, r, 0, 0, 0)
//        } catch (e: NoSuchAlgorithmException) {
//            throw ValidationException(e)
//        } catch (e: NoSuchProviderException) {
//            throw ValidationException(e)
//        } catch (e: InvalidKeyException) {
//            throw ValidationException(e)
//        }


    }


    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit
    abstract suspend fun sendTo(address: String, amount: Long): Unit
}

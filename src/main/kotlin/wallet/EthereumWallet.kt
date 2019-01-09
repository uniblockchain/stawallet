package wallet

import config
import jetbrains.exodus.entitystore.PersistentEntityStores
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigInteger
import jetbrains.exodus.core.dataStructures.hash.HashUtil
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder


class EthereumWallet(val coldAddress: String, val hotXPrv: String) {

    val coin = "eth"

    private val database = object {
        val store = PersistentEntityStores.newInstance("${config.getString("db.envPath")}/$coin")!!

        val TYPE_UTXO = "utxo"
        val TYPE_UTXO_TXHASH = "txhash"
        val TYPE_UTXO_AMOUNT = "utxo"
        val TYPE_UTXO_VOUT = "vout"

    }

    fun generateReceivingAddress(): String {
        return "" // TODO
    }

    fun sendTo(address: String, amountToSend: BigInteger): String {

//        val privKey = HashUtil.sha3("cat".toByteArray())
//        val ecKey = ECKey.fromPrivate(privKey)
//
//        val senderPrivKey = HashUtil.sha3("cow".toByteArray())
//
//        val gasPrice = Hex.decode("09184e72a000")
//        val gas = Hex.decode("4255")


//        val transaction: Transaction(null, gasPrice, gas, ecKey.getAddress(), amountToSend.toByteArray(), null)

        RawTransaction.createEtherTransaction(
            BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, address,
            BigInteger.valueOf(Long.MAX_VALUE)
        )
        return "" // TODO
    }

}

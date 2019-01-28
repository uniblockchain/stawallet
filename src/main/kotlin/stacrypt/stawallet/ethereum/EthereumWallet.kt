package stacrypt.stawallet.ethereum

import java.math.BigInteger
import org.web3j.crypto.RawTransaction
import redis.clients.jedis.Jedis


class EthereumWallet(val coldAddress: String, val hotXPrv: String) {

    val coin = "eth"

    /**
     * Redis data structure for Bitcoin wallet:
     *
     * * Last watched block height       : "eth:block:pointer" : value
     *      Addresses from index 0 to this pointer should actively be watched
     *
     * * Last Issued Hot Address Index   : "eth:addr:pointer" : value
     *      Addresses from index 0 to this pointer should actively be watched
     *
     * * Deposit  History                : "eth:deposits:{address}" : hashMap(txid, amount, warmTxid, coldTxid)
     *      We just record fully-confirmed transactions here
     *
     * * Withdraw History                : "eth:withdraws:{address}" : set(txid)
     *
     * * Invoice  History                : "eth:invoices:{invoiceId}" : txid
     *      InvoiceId is a unique id which sent by client to prevent double withdrawal
     *
     */
    private val database = object {

        private val jedis = Jedis("localhost")
        private val PREFIX = coin
        private val KEY_UTXO = "utxo"

//        var balance: Int
//            get() = stacrypt.stawallet.getJedis.za
//            set() = {}
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


//        val transaction: stacrypt.stawallet.bitcoin.Transaction(null, gasPrice, gas, ecKey.getAddress(), amountToSend.toByteArray(), null)

        RawTransaction.createEtherTransaction(
            BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, address,
            BigInteger.valueOf(Long.MAX_VALUE)
        )
        return "" // TODO
    }

}
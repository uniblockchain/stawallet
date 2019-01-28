package stacrypt.stawallet

import redis.clients.jedis.Jedis
import java.util.*

abstract class RedisStorage(val name: String) {

    val jedis: Jedis = Jedis(config.getString("storage.redis.server"))

}

class UtxoStorage(name: String) : RedisStorage(name) {

    companion object {
//        val STORE_TYPE_UTXO = "utxo"
//        val STORE_TYPE_UTXO_TXHASH = "txhash"
//        val STORE_TYPE_UTXO_AMOUNT = "utxo"
//        val STORE_TYPE_UTXO_VOUT = "vout"

        val UTXO = "utxo"
        val ADDR = "addr"
        val TX = "tx"
        val DEPOSIT = "d"
        val WITHDRAW = "w"
        val CHANGE = "c"
        val COLD = "o"
    }


    /**
     * Redis data structure for Utxo wallet Storage:
     *
     * * UTXOs                    : "btc:utxo"            : sortedSet(transactionId:vout, amount)
     * * Deposit  Transactions Id : "btc:txi:d:{addr}"    : set
     * * Overflow Transactions Id : "btc:txi:o"           : list
     * * Deposit  Transactions    : "btc:tx:d:{txid}"     : hashMap("amount", "confirmationsLeft")
     * * Withdraw Transactions    : "btc:tx:w:{txid}"     : hashMap("address", "sentAmount", "feeAmount")
     * * Archived Addresses       : "btc:addr:a"          : sortedSet(address, index)
     * * Deposit  Addresses       : "btc:addr:d"          : sortedSet(address, index)
     * * Change   Addresses       : "btc:addr:c"          : sortedSet(address, index)
     * * Cold     Addresses       : "btc:addr:o"          : list(address)
     *
     */

    val archivedAddresses: SortedSet<String>? = null

    fun hotBalance(): Long = jedis.zrangeWithScores("$name:$UTXO", 0, -1).sumByLong { it.score.toLong() }

}
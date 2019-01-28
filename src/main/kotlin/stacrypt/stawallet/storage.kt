package stacrypt.stawallet

import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction
import stacrypt.stawallet.bitcoin.NotEnoughFundException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

abstract class RedisStorage(val name: String) {

    open val jedis: Jedis = Jedis(config.getString("storage.redis.server"))

}

class UtxoJedis(val name: String) : Jedis(config.getString("storage.redis.server")) {
    fun hotBalance(): Long? = stacrypt.stawallet.jedis.zrangeWithScores(
        "$name:${UtxoStorage.UTXO}",
        0,
        -1
    ).sumByLong { it.score.toLong() }

    fun selectUtxo(amountToSend: Long, baseFee: Long, feePerExtraUtxo: Long): ArrayList<Pair<String, Long>> {
        val outPuts: ArrayList<Pair<String, Long>> = ArrayList()
        var estimatedFee = baseFee
        var totalInputAmount = 0L
        jedis.zrangeWithScores("$name:${UtxoStorage.UTXO}", 0, -1).forEach {
            totalInputAmount += it.score.toLong()
            estimatedFee += feePerExtraUtxo
            outPuts.add(Pair(it.element.toString(), it.score.toLong()))
            if (totalInputAmount >= amountToSend + estimatedFee) return outPuts
        }
        throw NotEnoughFundException(name, amountToSend)
    }

    fun removeUtxo(vararg utxo: String) = zrem("$name:$utxo", *utxo)


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

    override val jedis = UtxoJedis(name)


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

    @Synchronized
    fun <T> watch(exec: UtxoJedis.() -> T): T = watch("$name:*", exec = exec)


    @Synchronized
    private fun <T> watch(vararg keys: String, exec: UtxoJedis.() -> T): T {
        try {
            jedis.watch(*keys)
            return exec(jedis)
        } catch (e: Exception) {
            throw e
        } finally {
            jedis.unwatch()
        }
    }

    @Synchronized
    fun transaction(vararg keys: String, exec: Transaction.() -> Unit): List<Any>? {
        var result: List<Any>? = null
        watch(*keys) {
            val transaction = multi()
            try {
                exec(transaction)
                result = transaction.exec()
            } catch (e: Exception) {
                transaction.discard()
                throw e
            }
        }
        return result
    }
}


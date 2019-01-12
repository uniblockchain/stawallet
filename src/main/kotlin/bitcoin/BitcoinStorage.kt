import java.util.*

class UtxoStorage(walletName: String) : RedisStorage(walletName) {

    companion object {
        private val STORE_TYPE_UTXO = "utxo"
        private val STORE_TYPE_UTXO_TXHASH = "txhash"
        private val STORE_TYPE_UTXO_AMOUNT = "utxo"
        private val STORE_TYPE_UTXO_VOUT = "vout"

    }


    /**
     * Redis data structure for Utxo Wallet Storage:
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



}
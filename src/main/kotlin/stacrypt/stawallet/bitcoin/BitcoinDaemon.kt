package stacrypt.stawallet.bitcoin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon

object Bitcoind : WalletDaemon() {
    override var status: DaemonState
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    val rpcClient: BitcoinRpcClient = BitcoinRpcClientFactory.createBitcoinClientWithDefaultConfig()

    /**
     * Satoshis per bytes
     */
    suspend fun fairTxFeeRate(): Long? = withContext(Dispatchers.Default) {
        rpcClient.estimateSmartFee(6).feerate?.btcToSat()
    }

}


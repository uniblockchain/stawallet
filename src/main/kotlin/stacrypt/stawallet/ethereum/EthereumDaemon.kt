package stacrypt.stawallet.ethereum

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon
import stacrypt.stawallet.config

object geth : WalletDaemon() {
    override var status: DaemonState = DaemonState.CONNECTING

    val rpcClient by lazy { createBitcoinClientWithDefaultConfig() }
}

fun createBitcoinClientWithDefaultConfig(): Web3j? {
    val user = config.getString("daemons.bitcoind.rpc.username")
    val password = config.getString("daemons.bitcoind.rpc.password")
    val host = config.getString("daemons.bitcoind.rpc.host")
    val port = config.getInt("daemons.bitcoind.rpc.port")
    val secure = config.getBoolean("daemons.bitcoind.rpc.secure")

    return Web3j.build(HttpService("http${if (secure) "s" else ""}://$host:$port/"))
}

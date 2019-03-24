package stacrypt.stawallet.ethereum

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon
import stacrypt.stawallet.config

object geth : WalletDaemon() {
    override var status: DaemonState = DaemonState.CONNECTING

//    val rpcClient by lazy {
//        val url = "${config.getString("daemons.geth.rpc.host")}:${config.getString("daemons.geth.rpc.port")}"
//        EthereumRPC(baseURL = url)
//    }
    val rpcClient by lazy { createGethClientWithDefaultConfig()!! }

}

fun createGethClientWithDefaultConfig(): Web3j? {
    val user = config.getString("daemons.geth.rpc.username")
    val password = config.getString("daemons.geth.rpc.password")
    val host = config.getString("daemons.geth.rpc.host")
    val port = config.getInt("daemons.geth.rpc.port")
    val secure = config.getBoolean("daemons.geth.rpc.secure")

    return Web3j.build(HttpService("http${if (secure) "s" else ""}://$host:$port/"))
}

package stacrypt.stawallet.ripple

import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon

object rippled : WalletDaemon() {
    override var status: DaemonState
        get() = DaemonState.CONNECTING
        set(value) {
            throw Exception("Read-Only")
        }

    val rpcClient: RippleRpcClient by lazy { RippleRpcClientFactory.createRippleClientWithDefaultConfig() }


}
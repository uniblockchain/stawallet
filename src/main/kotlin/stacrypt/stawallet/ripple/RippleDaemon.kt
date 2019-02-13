package stacrypt.stawallet.ripple

import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon

val rippled = object : WalletDaemon() {
    override var status: DaemonState
        get() = DaemonState.CONNECTING
        set(value) {throw Exception("Read-Only")}



}
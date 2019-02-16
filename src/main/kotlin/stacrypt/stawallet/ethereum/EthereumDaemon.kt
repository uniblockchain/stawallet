package stacrypt.stawallet.ethereum

import stacrypt.stawallet.DaemonState
import stacrypt.stawallet.WalletDaemon

object geth : WalletDaemon() {
    override var status: DaemonState = DaemonState.CONNECTING


}
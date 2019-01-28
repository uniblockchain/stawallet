package stacrypt.stawallet

enum class DaemonState {
    AT_YOUR_SERVICE, SYNCING, CONNECTING, STOPPED, FAILED;

    fun ensureState(statueToEnsure: DaemonState) = this == statueToEnsure
}

/**
 * This class is a bridge to connect to each cryptocurrency daemon service.
 *
 * It should used as singleton
 *
 */
abstract class WalletDaemon {
    abstract var status: DaemonState
    abstract fun createRpcClient(): RpcClient

}
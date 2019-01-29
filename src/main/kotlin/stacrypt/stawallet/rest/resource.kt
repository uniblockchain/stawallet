package stacrypt.stawallet.rest

import stacrypt.stawallet.model.Wallet

enum class ClientRole

fun Wallet.export(role: ClientRole? = null) {
    WalletResource(
        id = id.toString(),
        balance = WalletBalanceResource(balance, unconfirmedBalance),
        secret = WalletSecretResource(seedFingerprint, path),
        onchainStatus = null
    )
}

data class WalletBalanceResource(
    val confirmed: Long,
    val unconfirmed: Long
)

data class WalletOnchainStatus(
    val network: String,
    val isSynced: Boolean,
    val isAlive: Boolean,
    val status: String
)

data class WalletSecretResource(
    val seedFingerprint: String,
    val path: String
)

data class WalletResource(
    val id: String,
    val balance: WalletBalanceResource,
    val secret: WalletSecretResource,
    val onchainStatus: WalletOnchainStatus?
)
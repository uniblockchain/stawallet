package stacrypt.stawallet.rest

import org.joda.time.DateTime
import stacrypt.stawallet.model.*

enum class ClientRole

fun WalletDao.export(role: ClientRole? = null): WalletResource {
    return WalletResource(
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

fun AddressDao.export(role: ClientRole? = null): AddressResource {
    return AddressResource(
        id = id.value,
        wallet = this.wallet.id.value,
        address = this.provision,
        isActive = this.isActive
    )
}

data class AddressResource(
    val id: Int,
    val wallet: String,
    val address: String,
    val isActive: Boolean
)

fun InvoiceDao.export(role: ClientRole? = null) =
    InvoiceResource(
        id = id.value,
        walletId = wallet.id.value,
        extra = extra,
        user = user,
        creation = creation,
        expiration = expiration,
        address = address.export(role)
    )


data class InvoiceResource(
    val id: Int,
    val walletId: String,
    val extra: String?,
    val user: String?,
    val creation: DateTime,
    val expiration: DateTime?,
    val address: AddressResource
)

fun DepositDao.export(role: ClientRole? = null, wallet: stacrypt.stawallet.Wallet): DepositResource {
    val confirmationsLeft = this.proof.blockHeight?.minus(wallet.latestBlockHeight)?.unaryMinus()
    return DepositResource(
        id = id.value,
        invoice = invoice.export(role),
        grossAmount = this.grossAmount,
        netAmount = this.netAmount,
        confirmationsLeft = confirmationsLeft,
        status = when {
            (confirmationsLeft != null) && (confirmationsLeft >= wallet.requiredConfirmations) -> DepositStatusResource.ACCEPTED
            (confirmationsLeft != null) && (confirmationsLeft < wallet.requiredConfirmations) -> DepositStatusResource.WAITING_TO_BE_CONFIRMED
            confirmationsLeft == null -> DepositStatusResource.ORPHAN
            else -> DepositStatusResource.FAILED
        },
        proof = this.proof.export(role, wallet),
        extra = this.extra
    )
}

fun ProofDao.export(role: ClientRole? = null, wallet: stacrypt.stawallet.Wallet) =
    ProofResource(
        txHash = this.txHash,
        blockHeight = this.blockHeight,
        blockHash = this.blockHash,
        link = wallet.blockchainExplorerTxLink(this.txHash)
    )

enum class DepositStatusResource {
    ACCEPTED, WAITING_TO_BE_CONFIRMED, ORPHAN, FAILED, UNACCEPTABLE
}

data class DepositResource(
    val id: Int,
    val invoice: InvoiceResource,
    val grossAmount: Long,
    val netAmount: Long,
    val proof: ProofResource,
    val confirmationsLeft: Int?,
    val status: DepositStatusResource,
    val extra: String?
)

data class ProofResource(
    val txHash: String?,
    val blockHash: String?,
    val blockHeight: Int?,
    val link: String?
)
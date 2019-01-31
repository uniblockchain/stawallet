package stacrypt.stawallet.rest

import org.joda.time.DateTime
import stacrypt.stawallet.model.AddressDao
import stacrypt.stawallet.model.InvoiceDao
import stacrypt.stawallet.model.WalletDao
import java.util.*

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

fun InvoiceDao.export(role: ClientRole? = null): InvoiceResource {
    return InvoiceResource(
        id = id.value,
        wallet = this.wallet.id.value,
        extra = this.extra,
        user = this.user,
        creation = this.creation,
        expiration = this.expiration,
        address = this.address.export(role)
    )
}

data class InvoiceResource(
    val id: Int,
    val wallet: String,
    val extra: String?,
    val user: String?,
    val creation: DateTime,
    val expiration: DateTime?,
    val address: AddressResource
)
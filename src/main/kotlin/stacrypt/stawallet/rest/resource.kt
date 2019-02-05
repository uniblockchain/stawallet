package stacrypt.stawallet.rest

import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import stacrypt.stawallet.model.AddressDao
import stacrypt.stawallet.model.DepositDao
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

fun DepositDao.export(role: ClientRole? = null) =
    DepositResource(
        id = id.value,
        invoice = invoice.export(role),
        grossAmount = grossAmount,
        netAmount = netAmount,
        confirmationsLeft = invoice.export(role),
        status = when{

        },
        error = error,
    )

data class DepositResource(
    val id: Int,
    val invoice: InvoiceResource,
    val grossAmount: Long,
    val netAmount: Long,
    val txhash: String,
    val confirmationsLeft: Int,
    val status: String?, // `orphan`, `confirmed`, `unconfirmed`, `failed`, `unacceptable`
    val error: String?
)

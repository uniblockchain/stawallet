package stacrypt.stawallet.rest

import org.joda.time.DateTime
import stacrypt.stawallet.model.*

enum class ClientRole

fun WalletDao.export(role: ClientRole? = null): WalletResource {
    return WalletResource(
        id = this.id.value,
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
        wallet = wallet.id.value,
        extra = extra,
        user = user,
        creation = creation,
        expiration = expiration,
        address = address.export(role)
    )


data class InvoiceResource(
    val id: Int,
    val wallet: String,
    val extra: String?,
    val user: String?,
    val creation: DateTime,
    val expiration: DateTime?,
    val address: AddressResource
)

fun DepositDao.export(role: ClientRole? = null, wallet: stacrypt.stawallet.Wallet): DepositResource {
    return DepositResource(
        id = id.value,
        invoice = invoice.export(role),
        grossAmount = this.grossAmount,
        netAmount = this.netAmount,
        isConfirmed = this.proof.confirmationsLeft == 0,
        status = ProofResource.status(this.proof),
        proof = this.proof.export(role, wallet),
        extra = this.extra
    )
}

fun ProofDao.export(role: ClientRole? = null, wallet: stacrypt.stawallet.Wallet) =
    ProofResource(
        txHash = this.txHash,
        blockHeight = this.blockHeight,
        blockHash = this.blockHash,
        link = wallet.blockchainExplorerTxLink(this.txHash),
        confirmationsLeft = this.confirmationsLeft,
        confirmationsTrace = (this.confirmationsTrace ?: "").split(","),
        extra = this.extra,
        error = this.error
    )

enum class DepositStatusResource {
    ACCEPTED, WAITING_TO_BE_CONFIRMED, ORPHAN, FAILED, UNACCEPTABLE
}

data class DepositResource(
    val id: Int?,
    val invoice: InvoiceResource,
    val grossAmount: Long,
    val netAmount: Long?,
    val proof: ProofResource,
    val isConfirmed: Boolean,
    val status: DepositStatusResource,
    val extra: String?
) {
    companion object {
        const val PAGE_SIZE = 20
    }
}

data class ProofResource(
    val txHash: String?,
    val blockHash: String?,
    val blockHeight: Int?,
    val confirmationsLeft: Int,
    val confirmationsTrace: List<String>,
    val link: String?,
    val extra: String?,
    val error: String?
) {
    companion object {

        fun status(proof: ProofDao?) = when {
            proof?.error != null -> DepositStatusResource.UNACCEPTABLE
            proof?.confirmationsLeft == null -> DepositStatusResource.ORPHAN
            proof.confirmationsLeft == 0 -> DepositStatusResource.ACCEPTED
            proof.confirmationsLeft > 0 -> DepositStatusResource.WAITING_TO_BE_CONFIRMED
            else -> DepositStatusResource.FAILED
        }

    }

}

fun TaskDao.export(role: ClientRole? = null, wallet: stacrypt.stawallet.Wallet): WithdrawResource =
    WithdrawResource(
        id = this.id.value,
        businessUid = this.businessUid,
        wallet = this.wallet.id.value,
        user = this.user,
        target = this.target,
        netAmount = this.netAmount,
        grossAmount = this.grossAmount,
        estimatedNetworkFee = this.estimatedNetworkFee,
        finalNetworkFee = this.finalNetworkFee,
        type = this.type.toString().toLowerCase(),
        isManual = when (this.status) {
            TaskStatus.QUEUED -> true
            TaskStatus.WAITING_LOW_BALANCE -> false
            TaskStatus.WAITING_MANUAL -> true
            else -> null
        },
        status = this.status.toString().toLowerCase(),
        txid = this.txid,
        issuedAt = this.issuedAt,
        paidAt = this.paidAt,
        trace = this.trace,
        proof = this.proof?.export(role, wallet)
    )


data class WithdrawResource(
    val id: Int,
    val businessUid: String,
    val wallet: String,
    val user: String?,
    val target: String,
    val netAmount: Long,
    val grossAmount: Long,
    val estimatedNetworkFee: Long,
    val finalNetworkFee: Long?,
    val type: String,
    val isManual: Boolean?,
    val status: String,
    val txid: String?,
    val proof: ProofResource?,
    val issuedAt: DateTime,
    val paidAt: DateTime?,
    val trace: String?
) {
    companion object {
        const val PAGE_SIZE = 20
    }

}

/**
 * RegExp to test a string for a ISO 8601 Date spec
 *  YYYY
 *  YYYY-MM
 *  YYYY-MM-DD
 *  YYYY-MM-DDThh:mmTZD
 *  YYYY-MM-DDThh:mm:ssTZD
 *  YYYY-MM-DDThh:mm:ss.sTZD
 * @see: https://www.w3.org/TR/NOTE-datetime
 * @type {RegExp}
 */
var ISO_8601 = "^\\d{4}(-\\d\\d(-\\d\\d(T\\d\\d:\\d\\d(:\\d\\d)?(\\.\\d+)?(([+-]\\d\\d:\\d\\d)|Z)?)?)?)?\$".toRegex()


/**
 * RegExp to test a string for a full ISO 8601 Date
 * Does not do any sort of date validation, only checks if the string is according to the ISO 8601 spec.
 *  YYYY-MM-DDThh:mm:ss
 *  YYYY-MM-DDThh:mm:ssTZD
 *  YYYY-MM-DDThh:mm:ss.sTZD
 * @see: https://www.w3.org/TR/NOTE-datetime
 * @type {RegExp}
 */
var ISO_8601_FULL = "^\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d+)?(([+-]\\d\\d:\\d\\d)|Z)?\$".toRegex()

fun String.isUtcDate() = ISO_8601.matches(this)
fun String.isFullUtcDate() = ISO_8601_FULL.matches(this)


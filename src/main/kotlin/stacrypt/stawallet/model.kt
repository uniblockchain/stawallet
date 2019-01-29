package stacrypt.stawallet

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table
import stacrypt.stawallet.InvoiceTable.default
import stacrypt.stawallet.InvoiceTable.nullable

enum class AddressSide { DEPOSIT, CHANGE, OVERFLOW, WITHDRAW }


//object walletTable : Table("wallet") {
//    val name = varchar("name", 32).primaryKey() // Unique name to identify the wallet (e.g. myitcoins)
//    val currency = varchar("currency", 12) // Short code of the related cryptocurrency (e.g. BTC)
//    val network = varchar("network", 32) // testnet, etc.
//}

/**
 *  Any key which being created to deposit, change... (just HOT wallet)
 */
object KeyTable : IntIdTable() {

    /**
     * Public Key in binary format
     */
    val wallet = binary("public", 512)

    /**
     * Public Key in binary format
     */
    val public = binary("public", 512)

    /**
     * The way address being shown in the related cryptocurrency
     */
    val provision = InvoiceTable.varchar("wif", 128)

    /**
     * BIP-44 derivation path (from the master seed)
     */
    val path = InvoiceTable.varchar("path", 32)

    /**
     * Should we watch for new transactions related to it
     */
    val watch = InvoiceTable.bool("watch").default(true)
}

enum class InvoicePurpose {
    /**
     * For clients to charge their account
     */
    DEPOSIT,

    /**
     * Generated for administrators to charge the hot wallet in case of low-balance
     */
    CHARGE,

    /**
     * Change address for utxo-based currencies
     */
    CHANGE
}

/**
 * Invoice for users to track their payments
 */
object InvoiceTable : IntIdTable("invoice") {
    val wallet = varchar("wallet", 12)
    val extra = varchar("data", 1_000).nullable() // invoiceId, data, payload, etc.
    val side = enumeration("side", AddressSide::class)
    val user = varchar("user", 128).nullable() // Id of the user (in Third-Party)
    val creation = datetime("creation").nullable() // time to be expired
    val expiration = datetime("expiration").nullable() // time to be expired
}

/**
 * Any payment which is found (just to our HOT wallet)
 */
object DepositTable : IntIdTable() {
    val invoice = integer("invoice").nullable() // Whether it is related to any issued invoice
    val amount = long("amount")
    val txid = varchar("txid", 256).nullable() // Origin of this deposit on the related blockchain
    val registered = InvoiceTable.bool("registered").default(false) // Is it registered on main server?
    val error = InvoiceTable.varchar("user", 128).nullable() // Reason of unacceptance, `null` means acceptable
}

enum class TaskType {
    /**
     * Transfer to someone of our client
     */
    WITHDRAW,

    /**
     * Transfer to cold wallet
     */
    OVERFLOW,

    /**
     * Manually export money by admins
     */
    DECHARGE

}

enum class TaskStatus { FINISHED, CONFIRMING, PUSHED, WAITING_MANUAL, WAITING_LOW_BALANCE, ERROR, QUEUED }

/**
 * Any task which should be done by the blockchain
 */
object TaskTable : IntIdTable("task") {
    val wallet = varchar("wallet", 12)
    val taget = varchar("target", 128)
    val amount = long("amount")
    val type = enumeration("type", TaskType::class)
    val status = enumeration("status", TaskStatus::class)
    val txid = varchar("txid", 256).nullable()
    val trace = varchar("trace", 10_000)
}

enum class EventSide { RECEIVED, SENT }
enum class EventType { CONFIRMATION, DISCOVER }

object EventTable : IntIdTable("event") {
    val addressId = integer("address_id")
    val txid = varchar("txid", 256)
    val message = varchar("message", 256)
    val payload = varchar("payload", 256)
    val side = TaskTable.enumeration("side", EventSide::class)
    val type = TaskTable.enumeration("type", EventType::class)
}

object UtxoTable : IntIdTable("utxo") {
    val wallet = varchar("wallet", 12)
    val key = integer("key")
    val amount = long("amount")
    val txid = varchar("txid", 256)
    val vout = integer("vout")
}
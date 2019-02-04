package stacrypt.stawallet.model

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.joda.time.DateTime


/**
 * Each wallet could only handle one type of crypto-assets. But they may derived from a unique master seed
 */
object WalletTable : IdTable<String>("wallet") {

    /**
     * Unique name to identify the wallet (e.g. `my-lovely-bitcoins`)
     */
    override val id: Column<EntityID<String>> = varchar("id", 32).primaryKey().entityId()

    /**
     * Short code of the related cryptocurrency (e.g. `btc`). Always LOWERCASE
     */
    val currency = varchar("currency", 12) //

    /**
     * e.g. Testnet
     */
    val network = varchar("network", 32)

    /**
     * Fingerprint of the master seed
     */
    val seedFingerprint = varchar("seed_fingerprint", 64)

    /**
     * BIP-44 derivation path (from the master seed)
     */
    val path = varchar("path", 64)

    /**
     * Confirmed amount which is payable
     */
    val balance = long("balance").default(0)

    /**
     * Unconfirmed balance, which we are waiting for it to be confirmed in the new feature
     */
    val unconfirmedBalance = long("unconfirmed_balance").default(0)

}

/**
 *  Any key which being created to deposit, change... (just HOT wallet)
 */
object AddressTable : IntIdTable() {

    /**
     * Public Key in binary format
     */
    val wallet = reference("wallet", WalletTable)

    /**
     * Public Key in binary format
     */
    val publicKey = binary("public_key", 512)

    /**
     * The way address being shown in the related cryptocurrency
     */
    val provision = varchar("provision", 128)

    /**
     * BIP-44 derivation path (from the wallet seed)
     */
    val path = varchar("path", 32)

    /**
     * Should we watch for new transactions related to it
     */
    val isActive = bool("is_active").default(true)

    /**
     * This number is useful while making ethereum transactions
     */
    val nonce = integer("nonce").default(0)
}

/**
 * Invoice for users to track their payments
 */
object InvoiceTable : IntIdTable("invoice") {
    /**
     * The wallet of this invoice
     */
    val wallet = reference("wallet", WalletTable)

    /**
     * The address to be paid
     */
    val address = reference("address", AddressTable.id)

    /**
     * invoiceId, data, payload, etc.
     */
    val extra = varchar("data", 1_000).nullable()

    /**
     * This invoice issued for which user id? (user is just a reference and not related to us)
     */
    val user = varchar("user", 128).nullable()

    /**
     * The creation time
     */
    val creation = datetime("creation").default(DateTime.now())

    /**
     * The expiration time:
     * - In the past means expired
     * - null means not expired and doesnt have any expiration time
     * - In the future means will be expired at this time
     */
    val expiration = datetime("expiration").nullable()
}

/**
 * Any payment which is found (just to our HOT wallet)
 */
object DepositTable : IntIdTable() {
    /**
     * The invoice of the this deposit is based on (if any, might be anonymous. We will appreciate!)
     */
    val invoice = reference("invoice", InvoiceTable.id).nullable()

    /**
     * The amount we really received
     */
    val grossAmount = long("grossAmount")

    /**
     * Amount the user will be charged in our system (whether any fee or commission decreased)
     */
    val netAmount = long("netAmount")

    /**
     * Origin of this deposit on the related blockchain
     */
    val txid = varchar("txid", 256).nullable()

    /**
     * Reason of unacceptance, `null` means acceptable
     */
    val error = varchar("user", 128).nullable()
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

    /**
     * Pay from this wallet
     */
    val wallet = reference("wallet", WalletTable.id)

    /**
     * Pay to this address
     */
    val target = varchar("target", 128)

    /**
     * Amount to be sent
     */
    val amount = long("amount")

    /**
     * Why we are transfering this money
     */
    val type = enumeration("type", TaskType::class)

    /**
     * What is the currenct status of this task?
     */
    val status = enumeration("status", TaskStatus::class)

    /**
     * The related transaction id in the related blockchain
     */
    val txid = varchar("txid", 256).nullable()

    /**
     * Some type of stacktrace for the actions, errors, etc.
     */
    val trace = varchar("trace", 10_000)
}

enum class EventSide { RECEIVED, SENT }
enum class EventSeverity { INFO, WARNING, ERROR, KILLER }

/**
 * Whatever happened to the blockchain which has any effect on our wallet
 */
object EventTable : IntIdTable("event") {

    /**
     * Which wallet?
     */
    val wallet = reference("wallet", WalletTable.id)

    /**
     * Which key is related to?
     */
    val address = reference("address", AddressTable.id)

    /**
     * What is the transaction id in blockchain?
     */
    val txid = varchar("txid", 256).nullable()

    /**
     * Height of the block which this happened
     */
    val blocHeight = long("blockHeight").nullable()

    /**
     * Any message?
     */
    val message = varchar("message", 256).nullable()

    /**
     * Any extra data?
     */
    val payload = varchar("payload", 256).nullable()

    /**
     * Severity
     */
    val severity = enumeration("severity", EventSeverity::class).default(EventSeverity.INFO)

}

/**
 * Just for utxo-based wallets
 */
object UtxoTable : IntIdTable("utxo") {

    /**
     * Which wallet it belongs to
     */
    val wallet = reference("wallet", WalletTable.id)

    /**
     * Id of the related key
     */
    val address = reference("address", AddressTable.id)

    /**
     * Amount
     */
    val amount = long("amount")

    /**
     * Origin transaction id in the blockchain
     */
    val txid = varchar("txid", 256)

    /**
     * Index of this utxo in the origin transaction's input
     */
    val vout = integer("vout")

    /**
     * Index of this utxo in the origin transaction's input
     */
    val isSpent = bool("isSpent").default(false)
}

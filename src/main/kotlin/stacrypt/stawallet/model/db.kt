package stacrypt.stawallet.model

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.joda.time.DateTime
import stacrypt.stawallet.model.InvoiceTable.nullable


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
    val address = reference("address", AddressTable)

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
 * It is a proof for a reality of a transaction or deposit or etc. in the real world blockchain.
 * Contains the block details, transaction detail and more.
 *
 * Note: These records are accepted unless is referred by a `deposit`.
 * So we can find the information of pending or unaccepted transactions here
 */
object ProofTable : IntIdTable("proof") {

    /**
     * The invoice of the this deposit is based on
     */
    val invoice = reference("invoice", InvoiceTable)

    /**
     * Exact amount in the transaction
     */
    val amount = varchar("amount", 32)

    /**
     * Block hash of where the transaction located at
     */
    val txHash = varchar("tx_hash", 256)

    /**
     * Block hash of where the transaction located at
     */
    val blockHash = varchar("block_hash", 256).nullable()

    /**
     * Block height of where the transaction located at
     */
    val blockHeight = integer("block_height").nullable()

    /**
     * Extra information, link, etc
     */
    val extra = varchar("extra", 1_000).nullable()

    /**
     * Reason of unacceptability (`null` means `without-error`)
     */
    val error = varchar("error", 1_000).nullable()

}

/**
 * Any payment which is found (just to our HOT wallet). These deposits are 100% accepted by us.
 */
object DepositTable : IntIdTable() {

    /**
     * The invoice of the this deposit is based on
     */
    val invoice = reference("invoice", InvoiceTable)

    /**
     * The proof of this deposit on the related blockchain on the real world
     *
     * Note: It will be filled automatically by blockchain watcher
     */
    val proof = reference("proof", ProofTable)

    /**
     * The amount we really received
     */
    val grossAmount = long("gross_amount")

    /**
     * Amount the user will be charged in our system (whether any fee or commission decreased)
     */
    val netAmount = long("net_amount")

    /**
     * Extra information (if required)
     */
    val extra = varchar("extra", 1_000).nullable()
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

enum class TaskStatus {

    /**
     * The transaction pushed and confirmed in the network successfully
     */
    FINISHED,

    /**
     * The transaction has been mined and we are waiting for required confirmation
     */
    CONFIRMING,

    /**
     * The transaction has been pushed to the network, but has not been mined yet
     */
    PUSHED,

    /**
     * This transaction should be sent manually by admins (The transaction has not been created yet)
     */
    WAITING_MANUAL,

    /**
     * This transaction should be sent automatically but the balance is not enough
     * (The transaction has not been created yet)
     */
    WAITING_LOW_BALANCE,

    /**
     * There is an error with this transaction
     */
    ERROR,

    /**
     * This transaction should be sent automatically ASAP (The transaction has not been created yet)
     */
    QUEUED
}

/**
 * Any task which should be done by the blockchain
 */
object TaskTable : IntIdTable("task") {

    /**
     * Pay from this wallet
     */
    val wallet = reference("wallet", WalletTable)

    /**
     * It's a mandatory UNIQUE identifier which prevents double-spend
     */
    val businessUid = varchar("business_uid", 100).uniqueIndex("business_uid_uniqueness")

    /**
     * Pay to this address
     */
    val target = varchar("target", 128)

    /**
     * This invoice issued for which user id? (user is just a reference and not related to us)
     *
     * Best practice per `type`:
     * * WITHDRAW -> Target user
     * * DECHARGE -> In charge admin user
     * * OVERFLOW -> `null`
     */
    val user = InvoiceTable.varchar("user", 128).nullable()

    /**
     * Amount to be sent
     */
    val netAmount = long("net_amount")

    /**
     * Amount the user will be charged in our system (whether any fee or commission decreased)
     *
     * Note: The network fee is not related to this field
     *
     * Note 2: It is JUST to log what happened. It means that this number has no meaning to us.
     */
    val grossAmount = long("gross_amount")

    /**
     * We estimate this number when we want to issue a withdraw records
     *
     * Note: It is JUST to log what happened. It means that this number has no meaning to us.
     *
     * Note 2: We STRONGLY recommend to calculate this number before issue a withdraw record (there is a function estimate fee)
     */
    val estimatedNetworkFee = long("estimated_fee")

    /**
     * Final network fee
     *
     * It will be calculated when we pushed the transaction to the network
     *
     */
    val finalNetworkFee = long("final_network_fee").nullable()

    /**
     * Why we are transferring this money
     */
    val type = enumerationByName("type", 50, TaskType::class)

    /**
     * What is the current status of this task?
     */
    val status = enumerationByName("status", 50, TaskStatus::class).default(TaskStatus.QUEUED)

    /**
     * The related transaction id in the related blockchain
     */
    val txid = varchar("txid", 256).nullable()

    /**
     * Some type of stacktrace for the actions, errors, etc.
     */
    val trace = varchar("trace", 10_000)

    /**
     * The proof of this transaction in the real world blockchain network
     *
     * Note: It will be filled automatically by blockchain watcher
     */
    val proof = reference("proof", ProofTable).nullable()

    /**
     * The creation time
     */
    val issuedAt = datetime("issued_at").default(DateTime.now())

    /**
     * Time of pushing the transaction into network:
     */
    val paidAt = datetime("paid_at").nullable()

}

/**
 * Just for utxo-based wallets
 */
object UtxoTable : IntIdTable("utxo") {

    /**
     * Which wallet it belongs to
     */
    val wallet = reference("wallet", WalletTable)

    /**
     * Id of the related address
     */
    val address = reference("address", AddressTable)

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
     * The proof of where we found this on the real world
     *
     * Note: It will be filled automatically by blockchain watcher
     */
    val discoveryProof = reference("discovery_proof", ProofTable).nullable()

    /**
     * The proof of where we spent this on the real world
     *
     * Note: It will be filled automatically by blockchain watcher
     */
    val spendProof = reference("spend_proof", ProofTable).nullable()

    /**
     * Index of this utxo in the origin transaction's input
     */
    val isSpent = bool("isSpent").default(false)
}

package stacrypt.stawallet

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table
import stacrypt.stawallet.addressTable.nullable

enum class AddressSide {
    DEPOSIT, CHANGE, OVERFLOW, WITHDRAW
}

object addressTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val wif = varchar("wif", 128)
    val path = varchar("path", 32)
    val side = enumeration("side", AddressSide::class)
    val archived = bool("archived").default(false)
    val data = varchar("data", 1_000).nullable()
    val userId = varchar("user_id", 128).nullable()
}

enum class TaskType {
    WITHDRAW, OVERFLOW
}

enum class TaskStatus {
    FINISHED, CONFIRMING, PUSHED, WAITING_MANUAL, WAITING_LOW_BALANCE, ERROR, QUEUED
}

object taskTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val taget = varchar("target", 128)
    val amount = long("amount")
    val type = enumeration("type", TaskType::class)
    val txid = eventTable.varchar("txid", 256).nullable()
    val trace = varchar("trace", 10_000)
}

object depositTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val amount = long("amount")
    val userId = varchar("user_id", 128).nullable()
    val addressId = integer("address_id")
    val invoiceId = varchar("invoice_id", 64)
    val trace = varchar("trace", 10_000)
}

enum class EventSide {
    RECEIVED, SENT
}

enum class EventType {
    CONFIRMATION, DISCOVER
}

object eventTable : IntIdTable() {
    val addressId = integer("address_id")

    val txid = varchar("txid", 256)
}

object utxoTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val addressId = integer("address_id")
    val amount = long("amount")
    val txid = varchar("txid", 256)
    val vout = integer("vout")
}
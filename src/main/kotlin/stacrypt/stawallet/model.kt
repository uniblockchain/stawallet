package stacrypt.stawallet

import org.jetbrains.exposed.dao.IntIdTable

enum class AddressSide { DEPOSIT, CHANGE, OVERFLOW, WITHDRAW }

object AddressTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val wif = varchar("wif", 128)
    val path = varchar("path", 32)
    val side = enumeration("side", AddressSide::class)
    val archived = bool("archived").default(false)
    val data = varchar("data", 1_000).nullable()
    val userId = varchar("user_id", 128).nullable()
}

enum class TaskType { WITHDRAW, OVERFLOW }
enum class TaskStatus { FINISHED, CONFIRMING, PUSHED, WAITING_MANUAL, WAITING_LOW_BALANCE, ERROR, QUEUED }

object TaskTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val taget = varchar("target", 128)
    val amount = long("amount")
    val type = enumeration("type", TaskType::class)
    val status = enumeration("status", TaskStatus::class)
    val txid = varchar("txid", 256).nullable()
    val trace = varchar("trace", 10_000)
}

object DepositTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val amount = long("amount")
    val addressId = integer("address_id")
    val txid = varchar("txid", 256).nullable()
}

enum class EventSide { RECEIVED, SENT }
enum class EventType { CONFIRMATION, DISCOVER }

object EventTable : IntIdTable() {
    val addressId = integer("address_id") 
    val txid = varchar("txid", 256)
    val message = varchar("message", 256)
    val payload = varchar("payload", 256)
    val side = TaskTable.enumeration("side", EventSide::class)
    val type = TaskTable.enumeration("type", EventType::class)
}

object UtxoTable : IntIdTable() {
    val wallet = varchar("wallet", 12)
    val addressId = integer("address_id")
    val amount = long("amount")
    val txid = varchar("txid", 256)
    val vout = integer("vout")
}
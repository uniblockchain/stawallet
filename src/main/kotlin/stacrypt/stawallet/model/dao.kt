package stacrypt.stawallet.model

import org.jetbrains.exposed.dao.*

class WalletDao(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, WalletDao>(WalletTable)

    var currency by WalletTable.currency
    var network by WalletTable.network
    var seedFingerprint by WalletTable.seedFingerprint
    var path by WalletTable.path
    var balance by WalletTable.balance
    var unconfirmedBalance by WalletTable.unconfirmedBalance

}

class AddressDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AddressDao>(AddressTable)

    var wallet by WalletDao referencedOn AddressTable.wallet
    var publicKey by AddressTable.publicKey
    var provision by AddressTable.provision
    var path by AddressTable.path
    var isActive by AddressTable.isActive
    var nonce by AddressTable.nonce
}

class InvoiceDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InvoiceDao>(InvoiceTable)

    var wallet by WalletDao referencedOn InvoiceTable.wallet
    var address by AddressDao referencedOn InvoiceTable.address
    var extra by InvoiceTable.extra
    var user by InvoiceTable.user
    var creation by InvoiceTable.creation
    var expiration by InvoiceTable.expiration
}

class TaskDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskDao>(TaskTable)

    var wallet by WalletDao referencedOn TaskTable.wallet
    var target by TaskTable.target
    var amount by TaskTable.amount
    var type by TaskTable.type
    var status by TaskTable.status
    var txid by TaskTable.txid
}

class DepositDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DepositDao>(DepositTable)

    var invoice by InvoiceDao referencedOn DepositTable.invoice
    var grossAmount by DepositTable.grossAmount
    var netAmount by DepositTable.netAmount
    var txid by DepositTable.txid
    var error by DepositTable.error
}

class EventDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EventDao>(EventTable)

    var wallet by WalletDao referencedOn EventTable.wallet
    var address by AddressDao referencedOn EventTable.address
    var txid by EventTable.txid
    var blocHeight by EventTable.blocHeight
    var message by EventTable.message
    var payload by EventTable.payload
    var severity by EventTable.severity
}

class UtxoDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UtxoDao>(UtxoTable)

    var wallet by WalletDao referencedOn UtxoTable.wallet
    var address by AddressDao referencedOn UtxoTable.address
    var amount by UtxoTable.amount
    var txid by UtxoTable.txid
    var vout by UtxoTable.vout
    var isSpent by UtxoTable.isSpent
}

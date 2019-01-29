package stacrypt.stawallet.model

import org.jetbrains.exposed.dao.*

class Wallet(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Wallet>(WalletTable)

    var currency by WalletTable.currency
    var network by WalletTable.network
    var seedFingerprint by WalletTable.seedFingerprint
    var path by WalletTable.path
    var balance by WalletTable.balance
    var unconfirmedBalance by WalletTable.unconfirmedBalance

}

class Address(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Address>(AddressTable)

    var wallet by Wallet referencedOn AddressTable.wallet
    var publicKey by AddressTable.publicKey
    var provision by AddressTable.provision
    var path by AddressTable.path
    var isActive by AddressTable.isActive
    var nonce by AddressTable.nonce
}

class Invoice(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Invoice>(InvoiceTable)

    var wallet by Wallet referencedOn InvoiceTable.wallet
    var extra by InvoiceTable.extra
    var purpose by InvoiceTable.purpose
    val user by InvoiceTable.user
    val creation by InvoiceTable.creation
    val expiration by InvoiceTable.expiration
}

class Task(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Task>(TaskTable)

    var wallet by Wallet referencedOn TaskTable.wallet
    var target by TaskTable.target
    var amount by TaskTable.amount
    var type by TaskTable.type
    var status by TaskTable.status
    var txid by TaskTable.txid
}

class Event(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Event>(EventTable)

    var wallet by Wallet referencedOn EventTable.wallet
    var address by Address referencedOn EventTable.addressId
    var txid by EventTable.txid
    var blocHeight by EventTable.blocHeight
    var message by EventTable.message
    var payload by EventTable.payload
    var severity by EventTable.severity
}

class Utxo(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Utxo>(UtxoTable)

    var wallet by Wallet referencedOn UtxoTable.wallet
    var keyId by Address referencedOn UtxoTable.addressId
    var txid by UtxoTable.txid
    var vout by UtxoTable.vout
    var isSpent by UtxoTable.isSpent
}

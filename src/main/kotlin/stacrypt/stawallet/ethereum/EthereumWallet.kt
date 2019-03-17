package stacrypt.stawallet.ethereum

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.toAddress
import org.kethereum.functions.calculateHash
import org.kethereum.functions.encodeRLP
import org.kethereum.model.*
import org.walleth.khex.toHexString
import java.math.BigInteger
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import stacrypt.stawallet.*
import stacrypt.stawallet.model.*
import java.lang.Exception
import java.math.BigDecimal


const val NETWORK_MAINNET = "mainnet"
const val NETWORK_RINKEBY = "rinkeby"
const val NETWORK_KOVAN = "kovan"
const val NETWORK_ROPSTEN = "ropsten"

val DEFAULT_TRANSACTION_GAS_LIMIT = BigInteger("21000")

class AccountNonceMismatchException(message: String?) : Exception(message)

class EthereumWallet(
    name: String,
    network: String,
    override val requiredConfirmations: Int,
    secretProvider: SecretProvider
) :
    Wallet(name, secretProvider, network) {
    companion object {
        fun coinType() = 60
    }

    override fun startBlockchainWatcher(): BaseBlockchainWatcher {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopBlockchainWatcher() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val daemon = geth

    override fun blockchainExplorerTxLink(txId: String): String? = when (this.network) {
        NETWORK_MAINNET -> "https://etherscan.io/tx/$txId"
        else -> "https://${this.network}.etherscan.io/tx/$txId"
    }

    // TODO: Use a hard derived address for warm wallet
    private val theOnlyWarmAddress: AddressDao
        get() = AddressDao.wrapRow(
            AddressTable.select { AddressTable.wallet eq name }
                .andWhere { AddressTable.path eq secretProvider.makePath(0, 1) }
                .last()
        )

    private val latestConfirmedBlockNumber: BigInteger
        get() = daemon.rpcClient!!.ethBlockNumber().send().blockNumber - requiredConfirmations.toBigInteger()

    private val latestConfirmedWarmBalance: BigInteger
        get() = daemon.rpcClient!!.ethGetBalance(
            theOnlyWarmAddress.provision,
            DefaultBlockParameterNumber(latestConfirmedBlockNumber)
        ).send().balance

    /**
     * Usually there is only one invoice (and address) per user.
     * However, using this method, you will find it, or it will be created automatically if it doesn't exist.
     */
    private fun inquireInvoice(user: String): InvoiceDao {
        val q = InvoiceTable.innerJoin(AddressTable)
            .select { InvoiceTable.wallet eq name }
            .andWhere { InvoiceTable.user eq user }
            .andWhere { AddressTable.isActive eq true }
            .andWhere { InvoiceTable.expiration.isNull() or (InvoiceTable.expiration greater DateTime.now()) }
            .lastOrNull()

        if (q != null) return InvoiceDao.wrapRow(q)

        return InvoiceDao.new {
            this.wallet = WalletDao.findById(name)!!
            this.user = user
            this.address = newAddress()
        }
    }

    /**
     * Issue a new address
     */
    private fun newAddress(): AddressDao {
        val q = AddressTable
            .select { AddressTable.wallet eq name }
            .orderBy(AddressTable.id, false)
            .firstOrNull()

        var newIndex = 0
        if (q != null) {
            val lastIssuedAddress = AddressDao.wrapRow(q)
            newIndex = lastIssuedAddress.path.split("/").last().toInt() + 1
        }

        val newPath = secretProvider.makePath(newIndex, 0)
        val newPublicKey = secretProvider.getHotPublicKeyObject(newPath)

        return AddressDao.new {
            this.wallet = WalletDao.findById(name)!!
            this.publicKey = newPublicKey.key.toByteArray()
            this.provision = newPublicKey.toAddress().hex
            this.path = newPath
        }
    }

    override suspend fun syncBlockchain() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun lastUsableInvoice(user: String): InvoiceDao? = inquireInvoice(user)

    /**
     * Using this method you will expire the (all) user's invoice(s) (if any).
     * Then issue a new invoice (and address) for the user.
     *
     * *** WARNING:
     * Usually you shouldn't use this method, because we assume that there is only ONE invoice (and address) per user.
     *
     * *** WARNING:
     * If you use this method, user's deposits to the last recent address won't be watched anymore.
     */
    override suspend fun issueNewInvoice(user: String): InvoiceDao {
        InvoiceDao.wrapRows(
            InvoiceTable.select { InvoiceTable.wallet eq name }.andWhere { InvoiceTable.user eq user }
        ).forEach {
            it.expiration = DateTime.now()
            // TODO: Deactivate the related address
        }
        return inquireInvoice(user)
    }

    override suspend fun sendTo(address: String, amountToSend: BigInteger, tag: Any?): Any = transaction {

        val gasPrice = daemon.rpcClient!!.ethGasPrice().send().gasPrice!!
        val gasLimit = DEFAULT_TRANSACTION_GAS_LIMIT
        if ((amountToSend + (gasPrice * gasLimit)) > latestConfirmedWarmBalance) {
            throw NotEnoughFundException(name, (amountToSend + (gasPrice * gasLimit)).weiToEth())
        }

        val blockchainNonce = daemon.rpcClient!!.ethGetTransactionCount(
            theOnlyWarmAddress.provision, DefaultBlockParameterName.PENDING
        ).send().transactionCount
        val recordedNonce = theOnlyWarmAddress.nonce

        if (recordedNonce != blockchainNonce.toInt())
            throw AccountNonceMismatchException("recorded-nonce ($recordedNonce) != blockchain-nonce ($blockchainNonce)")

        val transaction = createTransactionWithDefaults(
            from = Address(theOnlyWarmAddress.provision),
            to = Address(address),
            value = amountToSend,
            gasLimit = gasLimit,
            nonce = blockchainNonce,
            gasPrice = gasPrice
        )
        val signature = secretProvider.getHotKeyPair(theOnlyWarmAddress.path).signMessage(transaction.calculateHash())
        val signedTransaction = SignedTransaction(transaction, signature)

        val result = daemon.rpcClient!!.ethSendRawTransaction(
            signedTransaction.encodeRLP().toHexString()
        ).send()

        theOnlyWarmAddress.nonce += 1

        if (result.hasError()) throw TransactionPushException(name, transaction.toString(), result.error.toString())

        return@transaction result.transactionHash

    }

    override fun initializeToDb(force: Boolean): WalletDao {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

fun BigInteger.weiToEth() = (this.toBigDecimal().divide(BigDecimal("1000000000000000000")))
package stacrypt.stawallet.ethereum

import com.typesafe.config.Config
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.kethereum.crypto.signMessage
import org.kethereum.functions.calculateHash
import org.kethereum.functions.encodeRLP
import org.kethereum.model.*
import org.walleth.khex.toHexString
import java.math.BigInteger
import stacrypt.stawallet.ConfigSecretProvider
import stacrypt.stawallet.Wallet
import stacrypt.stawallet.model.AddressDao
import stacrypt.stawallet.model.AddressTable
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.InvoiceDao
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import stacrypt.stawallet.NotEnoughFundException
import stacrypt.stawallet.TransactionPushException
import java.lang.Exception
import java.math.BigDecimal


const val NETWORK_MAINNET = "mainnet"
const val NETWORK_RINKEBY = "rinkeby"
const val NETWORK_KOVAN = "kovan"
const val NETWORK_ROPSTEN = "ropsten"

val DEFAULT_TRANSACTION_GAS_LIMIT = BigInteger("21000")

class AccountNonceMismatchException(message: String?) : Exception(message)

class EthereumWallet(name: String, config: Config, network: String) :
    Wallet(name, ConfigSecretProvider(config, 60), network) {
    override val daemon = geth

    override fun blockchainExplorerTxLink(txId: String): String? = when (this.network) {
        NETWORK_MAINNET -> "https://etherscan.io/tx/$txId"
        else -> "https://${this.network}.etherscan.io/tx/$txId"
    }

    override var requiredConfirmations = config.getInt("requiredConfirmations")

    private val theOnlyWarmAddress: AddressDao = AddressDao.wrapRow(
        AddressTable.select { AddressTable.wallet eq name }
            .andWhere { AddressTable.path eq secretProvider.makePath(0, null) }
            .last()
    )

    private val latestConfirmedBlockNumber: BigInteger
        get() = daemon.rpcClient!!.ethBlockNumber().send().blockNumber - requiredConfirmations.toBigInteger()

    private val latestConfirmedWarmBalance: BigInteger
        get() = daemon.rpcClient!!.ethGetBalance(
            theOnlyWarmAddress.provision,
            DefaultBlockParameterNumber(latestConfirmedBlockNumber)
        ).send().balance


    override suspend fun syncBlockchain() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun lastUsableInvoice(user: String): InvoiceDao? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun invoiceDeposits(invoiceId: Int): List<DepositDao> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun issueNewInvoice(user: String): InvoiceDao {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

}

fun BigInteger.weiToEth() = (this.toBigDecimal() / BigDecimal("1000000000000000000"))
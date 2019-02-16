package stacrypt.stawallet.ethereum

import com.typesafe.config.Config
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
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
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService


const val NETWORK_MAINNET = "mainnet"
const val NETWORK_RINKEBY = "rinkeby"
const val NETWORK_KOVAN = "kovan"
const val NETWORK_ROPSTEN = "ropsten"

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

    override suspend fun sendTo(address: String, amountToSend: BigInteger, tag: Any?): Any {

//        val privKey = HashUtil.sha3("cat".toByteArray())
//        val ecKey = ECKey.fromPrivate(privKey)
//
//        val senderPrivKey = HashUtil.sha3("cow".toByteArray())
//
//        val gasPrice = Hex.decode("09184e72a000")
//        val gas = Hex.decode("4255")


//        val transaction: stacrypt.stawallet.bitcoin.Transaction(null, gasPrice, gas, ecKey.getAddress(), amountToSend.toByteArray(), null)

        val transaction = createTransactionWithDefaults(
            from = Address(theOnlyWarmAddress.provision),
            to = Address(address),
            value = amountToSend
        )
        transaction.encodeRLP()
        val signature = secretProvider.getHotKeyPair(theOnlyWarmAddress.path).signMessage(transaction.calculateHash())
        val signedTransaction = SignedTransaction(transaction, signature)

        val web3 = Web3j.build(HttpService())  // defaults to http://localhost:8545/
        web3.ethSendRawTransaction(signedTransaction.encodeRLP().toHexString())

//        val rawTransaction = RawTransaction.createEtherTransaction(
//            BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, address,
//            BigInteger.valueOf(Long.MAX_VALUE)
//        )
//        val signedMessage: ByteArray = TransactionEncoder.signMessage(rawTransaction,);
//        String hexValue = Numeric . toHexString (signedMessage);

        return "" // TODO
    }

}

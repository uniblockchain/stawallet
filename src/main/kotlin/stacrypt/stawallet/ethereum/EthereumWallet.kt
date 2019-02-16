package stacrypt.stawallet.ethereum

import com.typesafe.config.Config
import java.math.BigInteger
import org.web3j.crypto.RawTransaction
import redis.clients.jedis.Jedis
import stacrypt.stawallet.ConfigSecretProvider
import stacrypt.stawallet.Wallet
import stacrypt.stawallet.WalletDaemon
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.InvoiceDao

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

    override val requiredConfirmations: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

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

    override suspend fun sendTo(address: String, amountToSend: Long, tag: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun sendTo(address: String, amountToSend: BigInteger): String {

//        val privKey = HashUtil.sha3("cat".toByteArray())
//        val ecKey = ECKey.fromPrivate(privKey)
//
//        val senderPrivKey = HashUtil.sha3("cow".toByteArray())
//
//        val gasPrice = Hex.decode("09184e72a000")
//        val gas = Hex.decode("4255")


//        val transaction: stacrypt.stawallet.bitcoin.Transaction(null, gasPrice, gas, ecKey.getAddress(), amountToSend.toByteArray(), null)

        RawTransaction.createEtherTransaction(
            BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, address,
            BigInteger.valueOf(Long.MAX_VALUE)
        )
        return "" // TODO
    }

}

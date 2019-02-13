package stacrypt.stawallet.ripple

import com.typesafe.config.Config
import stacrypt.stawallet.ConfigSecretProvider
import stacrypt.stawallet.Wallet
import stacrypt.stawallet.WalletDaemon
import stacrypt.stawallet.bitcoin.NETWORK_MAINNET
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.InvoiceDao

class RippleWallet(name: String, config: Config, network: String) : Wallet(
    name,
    ConfigSecretProvider(config, if (network == NETWORK_MAINNET) 0 else 1),
    network
) {
    override val daemon: WalletDaemon = rippled

    override fun blockchainExplorerTxLink(txId: String): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override suspend fun sendTo(address: String, amountToSend: Long): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
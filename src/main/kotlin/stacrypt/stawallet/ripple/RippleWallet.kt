package stacrypt.stawallet.ripple

import com.typesafe.config.Config
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.ConfigSecretProvider
import stacrypt.stawallet.NotEnoughFundException
import stacrypt.stawallet.Wallet
import stacrypt.stawallet.model.AddressDao
import stacrypt.stawallet.model.AddressTable
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.InvoiceDao
import kotlin.math.roundToLong

const val LEDGER_INDEX_VALIDATED = "validated"
const val NETWORK_TESTNET = "testnet"
const val NETWORK_MAINNET = "mainnet"

val XRP_MINIMUM_BALANCE = 20.0.xrpToDrops()

class RippleWallet(name: String, config: Config, network: String) : Wallet(
    name,
    ConfigSecretProvider(config, if (network == NETWORK_MAINNET) 144 else 1),
    network
) {
    override val daemon = rippled

    private val theOnlyHotAddress: AddressDao?
        get() = AddressDao.wrapRow(AddressTable.select { AddressTable.wallet eq name }.last())

    override fun blockchainExplorerTxLink(txId: String) =
        "https://xrpcharts.ripple.com/#/transactions/${txId.toUpperCase()}"

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

    override suspend fun sendTo(address: String, amountToSend: Int, tag: Any?): Any = transaction {
        val accountInfo = daemon.rpcClient.getAccountInfo(
            account = theOnlyHotAddress!!.provision,
            ledgerIndex = LEDGER_INDEX_VALIDATED
        )

        val requiredFee = daemon.rpcClient.fee().drops!!.minimumFee!!.toLong()
        if (accountInfo.result?.validated != true) {
            // TODO: Report to boss
            throw Exception("")
        }

        if (accountInfo.result.accountData.balance.toLong() < amountToSend.toLong() + requiredFee + XRP_MINIMUM_BALANCE) {
            throw NotEnoughFundException(name, amountToSend.toLong())
        }

        val signingResult = daemon.rpcClient.signTransaction(
            transaction = Transaction(
                account = theOnlyHotAddress!!.provision,
                destination = address,
                destinationTag = if (tag is Int) tag else 0,
                transactionType = TransactionType.Payment,
                amount = amountToSend.toString(),
                fee = requiredFee.toString()
            ),
            seed = secretProvider.getHotPublicKey(theOnlyHotAddress!!.path).toRippleSeed(),
            keyType = "secp256k1"
        ).result

        val submitResult = daemon.rpcClient.submitTransaction(signingResult!!.txBlob!!).result
        if (submitResult?.engineResult == EngineResult.TesSUCCESS && submitResult.engineResultCode == 0) {
            return@transaction submitResult.txJson!!.hash!!
        }

        throw Exception("Error") // TODO: Report to boss
    }

}


fun Double.xrpToDrops() = (this * 1_000_000.0).roundToLong()

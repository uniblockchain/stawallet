package stacrypt.stawallet.ripple

import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.*
import stacrypt.stawallet.model.AddressDao
import stacrypt.stawallet.model.AddressTable
import stacrypt.stawallet.model.DepositDao
import stacrypt.stawallet.model.InvoiceDao
import java.math.BigInteger
import kotlin.math.roundToLong

const val LEDGER_INDEX_VALIDATED = "validated"
const val NETWORK_TESTNET = "testnet"
const val NETWORK_MAINNET = "mainnet"

val XRP_MINIMUM_BALANCE = 20.0.xrpToDrops()

class RippleWallet(name: String, network: String, secretProvider: SecretProvider) : Wallet(
    name,
    secretProvider,
    network
) {
    override fun startBlockchainWatcher(): BaseBlockchainWatcher {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopBlockchainWatcher() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val daemon = rippled

    /**
     * This is the address which we receive all deposits. Then we redirect each transaction to our warm wallet
     * (after transfering the overflow value to our cold address).
     */
    private val theOnlyHotAddress: AddressDao?
        get() {
            val path = secretProvider.makePath(0, 0) // FIXME: Obtain next index
            val q = AddressTable.select { AddressTable.wallet eq name }
                .andWhere { AddressTable.isActive eq true }
                .lastOrNull()

            if (q != null) return AddressDao.wrapRow(q)
            return AddressDao.new {
                //                this.provision =
            }
        }

    /**
     * This is the address which we keep received deposits.
     * We use this address to withdraw to external addresses.
     */
    private val theOnlyWarmAddress: AddressDao?
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

    override suspend fun issueNewInvoice(user: String): InvoiceDao {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun sendTo(address: String, amountToSend: BigInteger, tag: Any?): Any = transaction {
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

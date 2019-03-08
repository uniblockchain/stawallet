package stacrypt.stawallet

import io.ktor.application.Application
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.walleth.khex.hexToByteArray
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import stacrypt.stawallet.NotEnoughFundException
import stacrypt.stawallet.ethereum.geth
import stacrypt.stawallet.model.*
import stacrypt.stawallet.wallets
import kotlin.test.*

@KtorExperimentalAPI
class EthereumWalletTest : BaseApiTest() {

    private lateinit var wallet1: WalletDao

    override fun configure() = super.configure().apply {
        put("db.salt", "fake-salt")
        put("wallets.test-eth-wallet.cryptocurrency", "ethereum")
        put("wallets.test-eth-wallet.type", "address")
        put("wallets.test-eth-wallet.network", "mainnet")
        put(
            "wallets.test-eth-wallet.seed",
            "0x5c6e14e58ad94121498ea9535795967a7b0339a7e3206fb2c9e52de0bb8c76dfd2e783435cbded4fc9939720386dee90db32b36bd56b85750c4d6825f8cc2e8a" // BIP39: `enhance before small`
        )
        put("wallets.test-eth-wallet.coldAddress", "0xa6289A91A7D81DAD0Db433aA0Da7fE47998A97Eb")
        put("wallets.test-eth-wallet.requiredConfirmations", "12")
    }

    @Before
    fun beforeTests() {
        mockkObject(geth)
        every { geth.rpcClient } returns Web3j.build(HttpService(""))
        every { geth.rpcClient!!.ethBlockNumber().send().blockNumber } returns 1111111.toBigInteger()
        every {
            geth.rpcClient!!.ethGetBalance("0x515a835Ae70Fa5210d0FbA2EE37503C88cC47c6c", any()).send().balance
        } returns 9999999999999999.toBigInteger()
        every { geth.rpcClient!!.ethGasPrice().send().gasPrice } returns 27000.toBigInteger()
        every {
            geth.rpcClient!!.ethGetTransactionCount(
                "0x515a835Ae70Fa5210d0FbA2EE37503C88cC47c6c",
                DefaultBlockParameterName.PENDING
            ).send().transactionCount
        } returns 0.toBigInteger()
        every {
            geth.rpcClient!!.ethSendRawTransaction("0xf8668082697882520894cdfea301e38cb48a419f66852ce4d71072ef78868502540be3ff801ba082bc715d342a367e80e8ec7a4ebe7f1dd8e966422971a3dabb5b4939484010e1a0314abfa071070e8fef682ad243a0f0907d87741dcd1971b07d628b7ec66c04b8")
                .send().hasError()
        } returns false
        every {
            geth.rpcClient!!.ethSendRawTransaction("0xf8668082697882520894cdfea301e38cb48a419f66852ce4d71072ef78868502540be3ff801ba082bc715d342a367e80e8ec7a4ebe7f1dd8e966422971a3dabb5b4939484010e1a0314abfa071070e8fef682ad243a0f0907d87741dcd1971b07d628b7ec66c04b8")
                .send().transactionHash
        } returns "0xe62e77dc4d3ffa3681cfbb434e42f25b242ff20ac897e6dba8d0b5447d7cb280"
    }

    @After
    fun afterTest() {
        unmockkAll()
    }


    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = WalletDao.new("test-eth-wallet") {
                this.blockchain = BlockchainDao.new {
                    this.currency = "eth"
                    this.network = stacrypt.stawallet.ethereum.NETWORK_MAINNET
                }
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/60'/0'"
            }
            flushCache()

            val theOnlyHotAddress = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/60'/0'/1/0"
                this.publicKey = "0x020626aa26d6b1d15fa8b17dde8cb04e7ca12c4bb272957d949e28baa71f0f9efa".hexToByteArray()
                this.provision = "0x515a835Ae70Fa5210d0FbA2EE37503C88cC47c6c"
            }

            val address1 = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/60'/0'/0/0"
                this.publicKey = "0x02433cf63464a22c28e66b8abe71c4b606f4185e02f640015f5ee1ba6f2bafe8fe".hexToByteArray()
                this.provision = "0xD4024C147F84af067a20C138C3A708157235192a"
            }

            val address2 = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/0'/0'/0/1"
                this.publicKey = "0x03ac536f306e4fb932a1b84a20b21a155232b2a3b53676c8682e83a5226fbea12d".hexToByteArray()
                this.provision = "0x5596cD578AD9420dFa7646a714b7D49ae70B98A0"
            }

            val invoice1 = InvoiceDao.new(1) {
                wallet = wallet1
                address = address2
                user = "1"
            }

            val deposit1 = DepositDao.new {
                invoice = invoice1
                proof = ProofDao.new {
                    this.blockchain = wallet1.blockchain
                    this.txHash = "0xdd1f195a7fd44781f40b6b485670668762a48f71aefe474f078b61515da5b11b"
                    this.blockHash = "0x04d59327166c691ef0eff327f464c3bb814ae4360d5da9733b820e99caf6d1ea"
                    this.blockHeight = 7291344
                    this.confirmationsLeft = 0
                }
                grossAmount = 198763
                netAmount = 198000
            }

            val withdraw1 = TaskDao.new(1) {
                wallet = wallet1
                businessUid = "c0d9c0a7-6eb4-4e03-a324-f53a8be1b789"
                user = "1"
                target = "0xb39950d20750d348b3160ba7109e7b29afa553ec"
                grossAmount = 65740000
                netAmount = 65020000
                estimatedNetworkFee = 50000
                type = TaskType.WITHDRAW
                status = TaskStatus.QUEUED
            }
        }

    }

    @Test
    fun testSendTo() {
        val ethereumWallet = wallets[0]

        assertFailsWith(NotEnoughFundException::class) {
            runBlocking {
                ethereumWallet.sendTo(
                    "0xcdfea301e38cb48a419f66852ce4d71072ef7886",
                    9999999999999999.toBigInteger(),
                    null
                )
            }
        }

        assertEquals("0xe62e77dc4d3ffa3681cfbb434e42f25b242ff20ac897e6dba8d0b5447d7cb280",
            runBlocking {
                ethereumWallet.sendTo("0xcdfea301e38cb48a419f66852ce4d71072ef7886", 9999999999.toBigInteger(), null)
            }
        )
    }

}

package stacrypt.stawallet

import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import stacrypt.stawallet.bitcoin.BitcoinRpcClientFactory
import stacrypt.stawallet.bitcoin.BitcoinWallet
import stacrypt.stawallet.bitcoin.bitcoind
import stacrypt.stawallet.model.BlockchainDao
import stacrypt.stawallet.model.WalletDao
import stacrypt.stawallet.wallets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@KtorExperimentalAPI
class WalletRestTest : BaseApiTest() {

    private val walletsUrl = "/wallets"

    private lateinit var wallet1: WalletDao
    private lateinit var wallet2: WalletDao

    override fun configure() = super.configure().apply {
        put("db.salt", "fake-salt")

        put("wallets.test-btc-wallet.cryptocurrency", "BTC")
        put("wallets.test-btc-wallet.network", "mainnet")
        put("wallets.test-btc-wallet.accountId", "0")
        put("wallets.test-btc-wallet.coldAddress", "coldAddress")
        put("wallets.test-btc-wallet.requiredConfirmations", "4")

        put("wallets.test-eth-wallet.cryptocurrency", "ETH")
        put("wallets.test-eth-wallet.network", "mainnet")
        put("wallets.test-eth-wallet.accountId", "0")
        put("wallets.test-eth-wallet.coldAddress", "0xa6289A91A7D81DAD0Db433aA0Da7fE47998A97Eb")
        put("wallets.test-eth-wallet.requiredConfirmations", "10")
    }

    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = WalletDao.new("test-btc-wallet") {
                this.blockchain = BlockchainDao.new {
                    this.currency = "BTC"
                    this.network = "testnet3"
                }
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/0'/0"
            }
            wallet2 = WalletDao.new("test-eth-wallet") {
                this.blockchain = BlockchainDao.new {
                    this.currency = "ETH"
                    this.network = "rinkeby"
                }
                this.seedFingerprint = "11:11:11:11:11:11:11:11"
                this.path = "m/44'/0'/0"
            }
        }
    }


    @Before
    fun beforeTests() {
        mockkObject(bitcoind)
        every { bitcoind.rpcClient } returns BitcoinRpcClientFactory.createBitcoinClient("", "", "", 0, false)
    }

    @After
    fun afterTest() {
        unmockkAll()
    }


    @Test
    fun testRoot() {
        testEngine!!.handleRequest(HttpMethod.Get, "/").apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun testWalletList(): Unit {
        testEngine!!.apply {
            handleRequest(HttpMethod.Get, walletsUrl) {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(2, response.content?.toJson()?.toList()?.size)
            }
        }
    }

    @Test
    fun testWalletById(): Unit {
        testEngine!!.apply {
            handleRequest(HttpMethod.Get, "$walletsUrl/test-btc-wallet") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("test-btc-wallet", response.content?.toJson()!!["id"].asText())
                assertNotNull(response.content?.toJson()!!["balance"].asText())
                assertNotNull(response.content?.toJson()!!["secret"].asText())
//                assertNotNull(response.content?.toJson()!!["onchainStatus"].asText())
            }
        }
    }

}

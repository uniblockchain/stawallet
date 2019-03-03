package com.perfect.apartmentrental

import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.walleth.khex.hexToByteArray
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.http.HttpService
import stacrypt.stawallet.ethereum.NETWORK_MAINNET
import stacrypt.stawallet.ethereum.geth
import stacrypt.stawallet.model.*
import kotlin.test.*

@KtorExperimentalAPI
class EthereumRestTest : BaseApiTest() {

    private val walletsUrl = "/wallets"
    private val depositsUrl = "/deposits"
    private val withdrawsUrl = "/withdraws"
    private val invoicesUrl = "/invoices"

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
            geth.rpcClient!!.ethGetBalance("", DefaultBlockParameterNumber(1111111 - 30)).send().balance
        } returns 0.toBigInteger()
        every { geth.rpcClient!!.ethGasPrice().send().gasPrice } returns 27000.toBigInteger()
        every {
            geth.rpcClient!!.ethGetTransactionCount("", DefaultBlockParameterName.PENDING).send().transactionCount
        } returns 76.toBigInteger()
        every { geth.rpcClient!!.ethSendRawTransaction("").send().transactionHash } returns ""
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
                    this.network = NETWORK_MAINNET
                }
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/60'/0'"
            }
            flushCache()

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

            val address3 = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/60'/0'/0/2"
                this.publicKey = "0x03b181a8277e553d95e8d5316536c1bd2e3aa78618b2aeb1663f3d25a1cc3590f3".hexToByteArray()
                this.provision = "0x3F9963963Dd111678c7a8A8b1c7b77517B61D6BF"
            }

            val invoice1 = InvoiceDao.new(1) {
                wallet = wallet1
                address = address2
                user = "1"
            }

            val deposit1 = DepositDao.new(1) {
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
    fun testInvoice() {
        testEngine!!.apply {
            handleRequest(HttpMethod.Get, "$walletsUrl/test-eth-wallet$invoicesUrl?user=1&page=0") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!![0]["id"].asText())
                assertNotNull(response.content?.toJson()!![0]["wallet"].asText())
                assertNotNull(response.content?.toJson()!![0]["user"].asText())
                assertNotNull(response.content?.toJson()!![0]["creation"].asText())
                assertNotNull(response.content?.toJson()!![0]["expiration"].asText())
                assertNotNull(response.content?.toJson()!![0]["address"]["id"].asText())
                assertNotNull(response.content?.toJson()!![0]["address"]["wallet"].asText())
                assertNotNull(response.content?.toJson()!![0]["address"]["address"].asText())
                assertNotNull(response.content?.toJson()!![0]["address"]["active"].asBoolean())
            }
        }

        testEngine!!.apply {
            handleRequest(HttpMethod.Get, "$walletsUrl/test-eth-wallet$invoicesUrl/1") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!!["id"].asText())
                assertNotNull(response.content?.toJson()!!["wallet"].asText())
                assertNotNull(response.content?.toJson()!!["user"].asText())
                assertNotNull(response.content?.toJson()!!["creation"].asText())
                assertNotNull(response.content?.toJson()!!["expiration"].asText())
                assertNotNull(response.content?.toJson()!!["address"]["id"])
                assertNotNull(response.content?.toJson()!!["address"]["wallet"])
                assertNotNull(response.content?.toJson()!!["address"]["address"])
                assertNotNull(response.content?.toJson()!!["address"]["active"])
            }
        }
    }

    @Test
    fun testNewInvoice() {
        // New invoice
        testEngine!!.apply {
            handleRequest(HttpMethod.Post, "$walletsUrl/test-eth-wallet$invoicesUrl") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    listOf(
                        "user" to "1"
                    ).formUrlEncode()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!!["id"].asText())
                assertNotNull(response.content?.toJson()!!["wallet"].asText())
                assertNotNull(response.content?.toJson()!!["user"].asText())
                assertNotNull(response.content?.toJson()!!["creation"].asText())
                assertNotNull(response.content?.toJson()!!["expiration"].asText())
                assertNotNull(response.content?.toJson()!!["address"]["id"].asText())
                assertNotNull(response.content?.toJson()!!["address"]["wallet"].asText())
                assertNotNull(response.content?.toJson()!!["address"]["address"].asText())
                assertNotNull(response.content?.toJson()!!["address"]["active"].asBoolean())
            }
        }

        // Call again to get conflict (because of existence of at least one unused invoice)
        testEngine!!.apply {
            handleRequest(HttpMethod.Post, "$walletsUrl/test-eth-wallet$invoicesUrl") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    listOf(
                        "user" to "1"
                    ).formUrlEncode()
                )
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }

        // Call again, but force to issue a new invoice (using `force` query string)
        testEngine!!.apply {
            handleRequest(HttpMethod.Post, "$walletsUrl/test-eth-wallet$invoicesUrl?force=true") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    listOf(
                        "user" to "1"
                    ).formUrlEncode()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }

    }

    @Test
    fun testWithdrawHistory() {
        testEngine!!.apply {
            handleRequest(HttpMethod.Get, "$walletsUrl/test-eth-wallet$withdrawsUrl?user=1&page=0") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, response.content?.toJson()?.toList()?.size)
                assertNotNull(response.content?.toJson()!![0]["id"].asText())
                assertNotNull(response.content?.toJson()!![0]["businessUid"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["wallet"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["user"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["target"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["netAmount"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["grossAmount"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["estimatedNetworkFee"]?.asText())
                assertTrue(response.content?.toJson()!![0]["finalNetworkFee"].isNull)
                assertNotNull(response.content?.toJson()!![0]["type"]?.asText())
                assertNotNull(response.content?.toJson()!![0]["status"]?.asText())
                assertTrue(response.content?.toJson()!![0]["txid"].isNull)
                assertTrue(response.content?.toJson()!![0]["proof"].isNull)
                assertNotNull(response.content?.toJson()!![0]["issuedAt"]?.asText())
                assertTrue(response.content?.toJson()!![0]["paidAt"].isNull)
            }
        }
    }

    @Test
    fun testWithdrawByBusinessId() {
        testEngine!!.apply {
            handleRequest(
                HttpMethod.Get,
                "$walletsUrl/test-eth-wallet$withdrawsUrl/c0d9c0a7-6eb4-4e03-a324-f53a8be1b789"
            ) {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, response.content?.toJson()!!["id"].asInt())
            }
        }
    }

    @Test
    fun testNewWithdraw() {
        val withdrawId: Int
        testEngine!!.apply {
            handleRequest(HttpMethod.Post, "$walletsUrl/test-eth-wallet$withdrawsUrl") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    listOf(
                        "user" to "1",
                        "businessUid" to "1AJbsFZ64EpEfS5UAjAfcUG8pH8Jn3rn1F",
                        "isManual" to "false",
                        "target" to "1AJbsFZ64EpEfS5UAjAfcUG8pH8Jn3rn1F",
                        "netAmount" to "93511223",
                        "grossAmount" to "93583223",
                        "estimatedNetworkFee" to "485385",
                        "type" to "withdraw"
                    ).formUrlEncode()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!!["id"].asInt())
                withdrawId = response.content?.toJson()!!["id"].asInt()
            }
        }

        // Change to manual
        testEngine!!.apply {
            handleRequest(HttpMethod.Put, "$walletsUrl/test-eth-wallet$withdrawsUrl/$withdrawId") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    listOf(
                        "isManual" to "true"
                    ).formUrlEncode()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!!["id"].asText())
                assertEquals("waiting_manual", response.content?.toJson()!!["status"].asText())
            }
        }

        // Insert withdraw details manually
        testEngine!!.apply {
            handleRequest(HttpMethod.Put, "$walletsUrl/test-eth-wallet$withdrawsUrl/$withdrawId") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    listOf(
                        "finalNetworkFee" to "485385",
                        "txid" to "b6f6991d03df0e2e04dafffcd6bc418aac66049e2cd74b80f14ac86db1e3f0da"
                    ).formUrlEncode()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!!["id"].asText())
                assertEquals("pushed", response.content?.toJson()!!["status"].asText())
            }
        }
    }

    @Test
    fun testDepositHistory(): Unit {

        testEngine!!.apply {
            handleRequest(HttpMethod.Get, "$walletsUrl/test-eth-wallet$depositsUrl?user=1&page=0") {
            }.apply {
                assertEquals(1, response.content?.toJson()?.toList()?.size)
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content?.toJson()!![0]["id"].asText())
                assertNotNull(response.content?.toJson()!![0]["grossAmount"].asLong())
                assertNotNull(response.content?.toJson()!![0]["netAmount"].asLong())
                assertNotNull(response.content?.toJson()!![0]["confirmed"].asBoolean())
                assertNotNull(response.content?.toJson()!![0]["status"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["txHash"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["blockHeight"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["blockHeight"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["blockHash"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["link"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["confirmationsLeft"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["extra"].asText())
                assertNotNull(response.content?.toJson()!![0]["proof"]["error"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["id"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["wallet"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["extra"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["user"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["creation"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["expiration"].asText())
                assertNotNull(response.content?.toJson()!![0]["invoice"]["address"].asText())
            }
        }

    }

    @Test
    fun testDepositById(): Unit {

        testEngine!!.apply {
            handleRequest(HttpMethod.Get, "$walletsUrl/test-eth-wallet$depositsUrl/1") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, response.content?.toJson()!!["id"].asInt())
            }
        }

    }


}

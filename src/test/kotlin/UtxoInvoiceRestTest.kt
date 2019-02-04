package com.perfect.apartmentrental

import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import stacrypt.stawallet.bitcoin.BitcoinRpcClientFactory
import stacrypt.stawallet.bitcoin.NETWORK_TESTNET_3
import stacrypt.stawallet.bitcoin.bitcoind
import stacrypt.stawallet.model.AddressDao
import stacrypt.stawallet.model.InvoiceDao
import stacrypt.stawallet.model.WalletDao


@KtorExperimentalAPI
class UtxoInvoiceRestTest : BaseApiTest() {

    private val walletsUrl = "/wallets"

    private lateinit var wallet1: WalletDao

    override fun configure() = super.configure().apply {
        put("db.salt", "fake-salt")
        put("wallets.test-btc-wallet.cryptocurrency", "bitcoin")
        put("wallets.test-btc-wallet.type", "utxo")
        put("wallets.test-btc-wallet.network", "testnet3")
        put(
            "wallets.test-btc-wallet.seed",
            "0x5c6e14e58ad94121498ea9535795967a7b0339a7e3206fb2c9e52de0bb8c76dfd2e783435cbded4fc9939720386dee90db32b36bd56b85750c4d6825f8cc2e8a" // BIP39: `enhance before small`
        )
        put("wallets.test-btc-wallet.coldAddress", "coldAddress")
        put("wallets.test-btc-wallet.requiredConfirmations", "4")
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


    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = WalletDao.new("test-btc-wallet") {
                this.currency = "btc"
                this.network = NETWORK_TESTNET_3
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/0'/0"
            }
        }

    }

    @Test
    fun testInvoicePost(): Unit {

        /**
         * First we try to make a utxo for a new user
         */
        testEngine!!.apply {
            handleRequest(Post, "$walletsUrl/test-btc-wallet/invoices") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("user" to "1").formUrlEncode())
            }.apply {
                /**
                 * The results should show us that we have new invoice as well as new address
                 */
                assertEquals(OK, response.status())
                assertEquals("test-btc-wallet", response.content?.toJson()!!["walletId"].asText())
                assertEquals("1", response.content?.toJson()!!["user"].asText())
                assertEquals(
                    "mhXKmTxYhA5bb6yVPs95RJ8J2ELWr6Qowp",
                    response.content?.toJson()!!["address"]["address"].asText()
                )
                assertNotNull(response.content?.toJson()!!["id"].asText())
                assertNotNull(response.content?.toJson()!!["creation"].asText())

                assertTrue(response.content?.toJson()!!["extra"].isNull)
                assertTrue(response.content?.toJson()!!["expiration"].isNull)
            }
        }


        /**
         * We try to generate a new invoice for the user. It should not be successful because the user already have an
         * unused invoice.
         */
        testEngine!!.apply {
            handleRequest(Post, "$walletsUrl/test-btc-wallet/invoices") {
                setBody(listOf("user" to "1").formUrlEncode())
            }.apply {
                assertEquals(Conflict, response.status())
            }
        }


    }

}

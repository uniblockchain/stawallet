package com.perfect.apartmentrental

import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import stacrypt.stawallet.model.WalletDao


@KtorExperimentalAPI
class UtxoInvoiceRestTest : BaseApiTest() {

    private val walletsUrl = "/wallets"

    private lateinit var wallet1: WalletDao
    private lateinit var wallet2: WalletDao

    override fun config(app: Application) {
        super.config(app)
        (app.environment.config as MapApplicationConfig).apply {
            put("db.salt", "fake-salt")
        }
    }

    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = WalletDao.new("test-btc-wallet") {
                this.currency = "btc"
                this.network = "testnet3"
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/0'/0"
            }
        }
    }

    @Test
    fun testRoot() {
        testEngine!!.handleRequest(Get, "/").apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun testInvoicePost(): Unit {

        /**
         * First we try to make a utxo for a new user
         */
        testEngine!!.apply {
            handleRequest(Post, "$walletsUrl/test-btc-wallet/invoices") {
                setBody(listOf("user" to "1").formUrlEncode())

            }.apply {
                /**
                 * The results should show us that we have new invoice as well as new address
                 */
                assertEquals(OK, response.status())
                assertEquals("test-btc-wallet", response.content?.toJson()!!["id"].asText())
                assertNotNull(response.content?.toJson()!!["balance"].asText())
                assertNotNull(response.content?.toJson()!!["secret"].asText())
            }
        }


        /**
         * We try to generate a new invoice for the user
         */
        testEngine!!.apply {
            handleRequest(Post, "$walletsUrl/test-btc-wallet/invoices") {
                setBody(listOf("user" to "1").formUrlEncode())

            }.apply {
                /**
                 * The results should show us that we have new invoice as well as new address
                 */
                assertEquals(OK, response.status())
                assertEquals("test-btc-wallet", response.content?.toJson()!!["id"].asText())
                assertNotNull(response.content?.toJson()!!["balance"].asText())
                assertNotNull(response.content?.toJson()!!["secret"].asText())
            }
        }




    }

}

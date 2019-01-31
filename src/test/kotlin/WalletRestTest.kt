package com.perfect.apartmentrental

import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import stacrypt.stawallet.model.WalletDao
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@KtorExperimentalAPI
class WalletRestTest : BaseApiTest() {

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
            wallet2 = WalletDao.new("test-eth-wallet") {
                this.currency = "eth"
                this.network = "rinkeby"
                this.seedFingerprint = "11:11:11:11:11:11:11:11"
                this.path = "m/44'/0'/0"
            }
        }
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

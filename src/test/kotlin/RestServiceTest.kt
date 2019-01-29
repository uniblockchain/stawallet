package com.perfect.apartmentrental

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.Test
import stacrypt.stawallet.model.Wallet
import java.util.*


@KtorExperimentalAPI
class RestServiceTest : BaseApiTest() {

    private val walletsUrl = "/wallets"

    private lateinit var wallet1: Wallet
    private lateinit var wallet2: Wallet

    override fun config(app: Application) {
        super.config(app)
        (app.environment.config as MapApplicationConfig).apply {
            put("db.salt", "fake-salt")
        }
    }

    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = Wallet.new("testwallet1") {
                this.currency = "btc"
                this.network = "testnet3"
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/0'/0"
            }
            wallet2 = Wallet.new("testwallet2") {
                this.currency = "eth"
                this.network = "rinkeby"
                this.seedFingerprint = "11:11:11:11:11:11:11:11"
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
    fun testWalletList(): Unit {
        testEngine!!.apply {
            handleRequest(Get, walletsUrl) {
            }.apply {
                assertEquals(OK, response.status())
                assertEquals(2, response.content?.toJson()?.toList()?.size)
            }
        }
    }

}

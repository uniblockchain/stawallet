package com.perfect.apartmentrental

import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
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

    override fun configure() = super.configure().apply {
        put("db.salt", "fake-salt")

        put("wallets.test-btc-wallet.cryptocurrency", "bitcoin")
        put("wallets.test-btc-wallet.type", "utxo")
        put("wallets.test-btc-wallet.network", "mainnet")
        put(
            "wallets.test-btc-wallet.seed",
            "0x35eb81ec1be69722d3dcb3155f3295eb01563ac970dacfe116401892b0173156312e7dbdfafcdb652651aa0ef474658c49cfe0ca43969becebaae2a438b7dbe1"
        )
        put("wallets.test-btc-wallet.coldAddress", "coldAddress")
        put("wallets.test-btc-wallet.requiredConfirmations", "4")

        put("wallets.test-eth-wallet.cryptocurrency", "ethereum")
        put("wallets.test-eth-wallet.type", "address")
        put(
            "wallets.test-eth-wallet.seed",
            "0x35eb81ec1be69722d3dcb3155f3295eb01563ac970dacfe116401892b0173156312e7dbdfafcdb652651aa0ef474658c49cfe0ca43969becebaae2a438b7dbe1"
        )
        put("wallets.test-eth-wallet.coldAddress", "0xa6289A91A7D81DAD0Db433aA0Da7fE47998A97Eb")
        put("wallets.test-eth-wallet.requiredConfirmations", "10")
    }

    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = WalletDao.new("test-btc-wallet") {
                this.currency = "btc"
                this.network = "isTestnet3"
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

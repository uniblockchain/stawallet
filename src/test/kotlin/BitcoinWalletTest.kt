package stacrypt.stawallet

import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.QueryParameter
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.walleth.khex.hexToByteArray
import stacrypt.stawallet.NotEnoughFundException
import stacrypt.stawallet.bitcoin.*
import stacrypt.stawallet.model.*
import stacrypt.stawallet.wallets
import java.math.BigInteger
import kotlin.test.*

@KtorExperimentalAPI
class BitcoinWalletTest : BaseApiTest() {

    private lateinit var wallet1: WalletDao

    override fun configure() = super.configure().apply {
        put("db.salt", "fake-salt")
        put(
            "secrets.hotSeed",
            "0x5c6e14e58ad94121498ea9535795967a7b0339a7e3206fb2c9e52de0bb8c76dfd2e783435cbded4fc9939720386dee90db32b36bd56b85750c4d6825f8cc2e8a" // BIP39: `enhance before small`
        )
        put("wallets.test-btc-wallet.cryptocurrency", "BTC")
        put("wallets.test-btc-wallet.network", "mainnet")
        put("wallets.test-btc-wallet.accountId", "0")
        put("wallets.test-btc-wallet.coldAddress", "coldAddress")
        put("wallets.test-btc-wallet.requiredConfirmations", "4")
    }

    @Before
    fun beforeTests() {
        mockkObject(bitcoind)
        every { bitcoind.rpcClient } returns BitcoinRpcClientFactory.createBitcoinClient("", "", "", 0, false)
        every { bitcoind.rpcClient.estimateSmartFee(6) } returns EstimateSmartResult(feerate = 0.00004560, blocks = 6)
        every {
            bitcoind.rpcClient.createRawTransaction(
                inputs = listOf(
                    OutPoint("ac63077e17aef2cf50718a1e7531b8710714dbfdc53424d627c175ddb119cde5", 0),
                    OutPoint("5061556f857e118aae8d948496f61f645e12cf7ca2a107f8e4ae78b535e86dfb", 1)
                ), outputs = mapOf(
                    "1KbcrHQfw54dVpMx7sp8V78yDk1WotGozn" to 8173831.toBigDecimal(),
                    "172GCPPDgvE7vX5LXuZDPfHvAT8yPEhr5Y" to 634559.toBigDecimal()
                )
            )
        } returns "0200000002e5cd19b1dd75c127d62434c5fddb140771b831751e8a7150cff2ae177e0763ac0000000000fffffffffb6de8" +
                "35b578aee4f807a1a27ccf125e641ff69684948dae8a117e856f5561500100000000ffffffff02002752d567e702001976a9" +
                "14cbfe55279bf0aeceaaafa5393bf717df39eaaf7488ac00df607ab63900001976a914420ddb1c8b15e9beebf21c526c1265" +
                "cd49b9320e88ac00000000"
        every {
            bitcoind.rpcClient.signRawTransactionWithKey(
                hexString = "0200000002e5cd19b1dd75c127d62434c5fddb140771b831751e8a7150cff2ae177e0763ac0000000000ffff" +
                        "fffffb6de835b578aee4f807a1a27ccf125e641ff69684948dae8a117e856f5561500100000000ffffffff020027" +
                        "52d567e702001976a914cbfe55279bf0aeceaaafa5393bf717df39eaaf7488ac00df607ab63900001976a914420d" +
                        "db1c8b15e9beebf21c526c1265cd49b9320e88ac00000000",
                privKeys = listOf(
                    "KxNCVv5CbGcPaGvDHTcN8V4a9dTzfp3CjDjMf14gPcypB7GBfW18",
                    "L4XghQx1yLmizft19PYg5g7uFceJPY2ZELXHSamdmEkgByWC4GJD"
                )
            )
        } returns SignTransactionResult(
            hex = "0100000002f579ae58b619acb6a1d2d7718774020a257fc876ece390a746c9ec0e99c9239d000000006a47304402201a01" +
                    "01673ddb98f7e80a1f01548bc302dc1f63a420e831792d894ab70390184a02205312353f80e4f11b9886e69411a94832" +
                    "8b144b8c3760ba291d1d2132f5229ca0012102f6aa1275cf3b5528b92810d1087fe7bc73d72288ce2b7cf717cdcd2daa" +
                    "38ba41ffffffff050dd1d1b3ac73bf787cc54b310532f608e76e5f9d3661f5d03b9ea227721409000000006a47304402" +
                    "206bd5aa6356110a604a98b22bb78bb682438fd7e284780210f50b065cd9f257800220625a36c1d12480938a6d6592ac" +
                    "b8ea13fdbaf50bfcf16c5d976f7818709c0ad0012102206a4500544e40633e49f067a0934863533145ed2f4e157f9813" +
                    "807ff848e288ffffffff0284590200000000001976a914a4735a41c3a9e35db18e2e27820a14329946245988ac783007" +
                    "000000000017a9149566ecefcf3b444abd5b1ebf12345fe216a804b98700000000",
            complete = true
        )
        every {
            bitcoind.rpcClient.sendRawTransaction(
                transaction = "0100000002f579ae58b619acb6a1d2d7718774020a257fc876ece390a746c9ec0e99c9239d000000006a47" +
                        "304402201a0101673ddb98f7e80a1f01548bc302dc1f63a420e831792d894ab70390184a02205312353f80e4f11b" +
                        "9886e69411a948328b144b8c3760ba291d1d2132f5229ca0012102f6aa1275cf3b5528b92810d1087fe7bc73d722" +
                        "88ce2b7cf717cdcd2daa38ba41ffffffff050dd1d1b3ac73bf787cc54b310532f608e76e5f9d3661f5d03b9ea227" +
                        "721409000000006a47304402206bd5aa6356110a604a98b22bb78bb682438fd7e284780210f50b065cd9f2578002" +
                        "20625a36c1d12480938a6d6592acb8ea13fdbaf50bfcf16c5d976f7818709c0ad0012102206a4500544e40633e49" +
                        "f067a0934863533145ed2f4e157f9813807ff848e288ffffffff0284590200000000001976a914a4735a41c3a9e3" +
                        "5db18e2e27820a14329946245988ac783007000000000017a9149566ecefcf3b444abd5b1ebf12345fe216a804b9" +
                        "8700000000"
            )
        } returns "7c6dd126a8d86dfadcb5ab17185973f01010062e7e94f49bbd8e597496c42824"
    }

    @After
    fun afterTest() {
        unmockkAll()
    }


    override fun mockup(app: Application) {
        super.mockup(app)

        transaction {
            wallet1 = WalletDao.new("test-btc-wallet") {
                this.blockchain = BlockchainDao.new {
                    this.currency = "BTC"
                    this.network = NETWORK_TESTNET_3
                }
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/0'/0'"
            }
            flushCache()

            val address1 = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/0'/0'/0/0"
                this.publicKey = "0x02395cc1c6b46fabb771188d007b5f9bde500888a1d1aae7baaac54ce0f5951fc4".hexToByteArray()
                this.provision = "1FT1C5QJY699upasj23GYNDghDbJZqmczN"
            }

            val address2 = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/0'/0'/0/1"
                this.publicKey = "0x022a104970d7f85c9c97e9d86c6bba7ef9243413b6cd7d94492ab587145a11e5c2".hexToByteArray()
                this.provision = "1D6CqUvHtQRXU4TZrrj5j1iofo8f4oXyLj"
            }

            val address3 = AddressDao.new {
                this.wallet = wallet1
                this.path = "m/44'/0'/0'/0/2"
                this.publicKey = "0x031a5107e2a3c3ea5dbb07e53b9da3488717c263d4fecd2aea602de457117410b3".hexToByteArray()
                this.provision = "1Mwz1i3MK7AruNFwF3X84FK4qMmpooLtZG"
            }

//            val address4 = AddressDao.new {
//                this.wallet = wallet1
//                this.path = "m/44'/0'/0/1/3"
//                this.publicKey = "0x02a88dec9ceeca376a2f75668c07fb3a87c825f2100af52f3b92b12e0d530bba3f".hexToByteArray()
//                this.provision = "172GCPPDgvE7vX5LXuZDPfHvAT8yPEhr5Y"
//            }

            val utxo1 = UtxoDao.new(1) {
                this.wallet = wallet1
                this.address = address1
                this.txid = "5061556f857e118aae8d948496f61f645e12cf7ca2a107f8e4ae78b535e86dfb"
                this.vout = 1
                this.amount = 2340000.toBigInteger()
                this.discoveryProof = ProofDao.new {
                    this.blockchain = wallet1.blockchain
                    this.txHash = "5061556f857e118aae8d948496f61f645e12cf7ca2a107f8e4ae78b535e86dfb"
                    this.blockHash = "000000000000000000188252ee9277e8f60482a91b7f3cc9a4a7fb75ded482a8"
                    this.blockHeight = 562456
                    this.confirmationsLeft = 0
                }
            }

            val utxo2 = UtxoDao.new(2) {
                this.wallet = wallet1
                this.address = address2
                this.txid = "ac63077e17aef2cf50718a1e7531b8710714dbfdc53424d627c175ddb119cde5"
                this.vout = 0
                this.amount = 8173830.toBigInteger()
                this.discoveryProof = ProofDao.new {
                    this.blockchain = wallet1.blockchain
                    this.txHash = "ac63077e17aef2cf50718a1e7531b8710714dbfdc53424d627c175ddb119cde5"
                    this.blockHash = "0000000000000000001abf4471a2619a8ea134286fa8cf7ff2b28f63c9d883a9"
                    this.blockHeight = 462456
                    this.confirmationsLeft = 0
                }
            }

            val utxo3 = UtxoDao.new(3) {
                this.wallet = wallet1
                this.address = address3
                this.txid = "3f3b970d786527051962dec390db5dcca0ce6896c9dadf3b28d8d72c271bb3ee"
                this.vout = 45
                this.amount = 999999999.toBigInteger()
                this.discoveryProof = ProofDao.new {
                    this.blockchain = wallet1.blockchain
                    this.txHash = "3f3b970d786527051962dec390db5dcca0ce6896c9dadf3b28d8d72c271bb3ee"
                    this.blockHash = "000000000000000000058250dbd4d44abbc0ec6c53b8000ca62c92520c8bf11c"
                    this.blockHeight = 345354
                    this.confirmationsLeft = 2
                }
            }

        }

    }

    @Test
    fun testSendTo() {
        val bitcoinWallet = wallets[0]

        assertFailsWith(NotEnoughFundException::class) {
            runBlocking {
                bitcoinWallet.sendTo("1KbcrHQfw54dVpMx7sp8V78yDk1WotGozn", 99999999.toBigInteger(), null)
            }
        }

        assertEquals("7c6dd126a8d86dfadcb5ab17185973f01010062e7e94f49bbd8e597496c42824",
            runBlocking {
                bitcoinWallet.sendTo("1KbcrHQfw54dVpMx7sp8V78yDk1WotGozn", 8173831.toBigInteger(), null)
            }
        )
    }

    @Test
    fun testOnReceive() {
        val bitcoinWallet = wallets[0]

        assertFailsWith(NotEnoughFundException::class) {
            runBlocking {
                bitcoinWallet.sendTo("1KbcrHQfw54dVpMx7sp8V78yDk1WotGozn", 99999999.toBigInteger(), null)
            }
        }

        assertEquals("7c6dd126a8d86dfadcb5ab17185973f01010062e7e94f49bbd8e597496c42824",
            runBlocking {
                bitcoinWallet.sendTo("1KbcrHQfw54dVpMx7sp8V78yDk1WotGozn", 8173831.toBigInteger(), null)
            }
        )
    }

}

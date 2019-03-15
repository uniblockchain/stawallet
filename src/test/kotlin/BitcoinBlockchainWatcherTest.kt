package stacrypt.stawallet

import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
class BitcoinBlockchainWatcherTest : BaseApiTest() {

    private lateinit var wallet1: WalletDao

    override fun configure() = super.configure().apply {
        put("db.salt", "fake-salt")
        put("wallets.test-btc-wallet.cryptocurrency", "bitcoin")
        put("wallets.test-btc-wallet.type", "utxo")
        put("wallets.test-btc-wallet.network", "mainnet")
        put(
            "wallets.test-btc-wallet.seed",
            "0x5c6e14e58ad94121498ea9535795967a7b0339a7e3206fb2c9e52de0bb8c76dfd2e783435cbded4fc9939720386dee90db32b36bd56b85750c4d6825f8cc2e8a" // BIP39: `enhance before small`
        )
        put("wallets.test-btc-wallet.coldAddress", "coldAddress")
        put("wallets.test-btc-wallet.requiredConfirmations", "4")

        put("daemons.bitcoind.watcher.blockWatchGap", "100")
        put("daemons.bitcoind.watcher.mempoolWatchGap", "100")
    }

    @Before
    fun beforeTests() {
        mockkClass(BitcoinRpcClient::class)
        mockkObject(bitcoind)
        every { bitcoind.rpcClient.estimateSmartFee(6) } returns EstimateSmartResult(feerate = 0.00004560, blocks = 6)
        every {
            bitcoind.rpcClient.getMempoolDescendants()
        } returns emptyList<Transaction>() // FIXME

        every {
            bitcoind.rpcClient.getBlockCount()
        } returns 123456

        every {
            bitcoind.rpcClient.getBlockHash(123457)
        } returns "0000000000000000000404014bd39d4d432cdb1d4e29cc340ccf0e53374e9763"

        every {
            bitcoind.rpcClient.getBlockHash(less(123457))
        } returns "0000000000000000000404014bd39d4d432cdb1d4e29cc340c00000000000000"

        every {
            bitcoind.rpcClient.getBlock(any())
        } returns BlockInfo(tx = emptyList())

        every {
            bitcoind.rpcClient.getBlockWithTransactions("0000000000000000000404014bd39d4d432cdb1d4e29cc340ccf0e53374e9763")
        } returns BlockInfoWithTransactions(
            hash = "0000000000000000000404014bd39d4d432cdb1d4e29cc340ccf0e53374e9763",
            tx = emptyList()
        )
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
                    this.currency = "btc"
                    this.network = NETWORK_TESTNET_3
                }
                this.seedFingerprint = "00:00:00:00:00:00:00:00"
                this.path = "m/44'/0'/0'"
                this.latestSyncedHeight = 123456
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
                this.amount = 2340000
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
                this.amount = 8173830
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
                this.amount = 999999999
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
    fun testWatcherThread() {
        val bitcoinWallet = wallets[0] as BitcoinWallet

        val watcher = bitcoinWallet.startBlockchainWatcher()

        /**
         * Is it running in the correct thread?
         */
        var watcherThread: Thread? = null
        runBlocking { launch(watcher.dispatcher) { watcherThread = Thread.currentThread() } }
        assertNotNull(watcherThread)
        assertTrue { watcherThread!!.name.startsWith("test-btc-wallet-watcher") } // FIXME: It could be better

        assertTrue { watcher.blockWatcherJob?.isActive!! }

        bitcoinWallet.stopBlockchainWatcher()

        assertFalse { watcher.blockWatcherJob?.isActive!! }

    }

    @Test
    fun testNewBlockAppears() {
        val bitcoinWallet = wallets[0] as BitcoinWallet

        bitcoinWallet.startBlockchainWatcher()

        every {
            bitcoind.rpcClient.getBlockCount()
        } returns 123457

        Thread.sleep(1000)

        // New block should be discovered

        verifyOrder {
            bitcoind.rpcClient.getBlockCount()
            bitcoind.rpcClient.getBlockHash(123457)
            bitcoind.rpcClient.getBlockWithTransactions("0000000000000000000404014bd39d4d432cdb1d4e29cc340ccf0e53374e9763")
            for (i in 0..4) {
                bitcoind.rpcClient.getBlockHash(123456 - i)
                bitcoind.rpcClient.getBlock(match { it.startsWith("00000000") })
            }
        }

        bitcoinWallet.stopBlockchainWatcher()

    }

}

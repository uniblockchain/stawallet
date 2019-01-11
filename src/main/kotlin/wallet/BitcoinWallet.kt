package wallet

import OutPoint
import RpcClientFactory
import config
import jetbrains.exodus.entitystore.PersistentEntityStores
import sumByLong
import java.math.BigDecimal
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.bind.DatatypeConverter
import java.security.MessageDigest
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet2Params
import org.bitcoinj.params.TestNet3Params
import java.util.Arrays.asList
import java.util.Locale
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import java.awt.SystemColor.info
import org.web3j.crypto.WalletFile.Crypto
import org.bitcoin.NativeSecp256k1.sign
import java.math.BigInteger
import com.oracle.util.Checksums.update
import io.github.novacrypto.bip32.ExtendedPrivateKey
import io.github.novacrypto.bip32.networks.Bitcoin
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.ECPoint
import redis.clients.jedis.Jedis
import java.nio.ByteBuffer
import java.security.Security


private val logger = Logger.getLogger("wallet.Wallet")

data class NotEnoughFundException(val coin: String, val amountToPay: Long = 0L) :
    Exception("wallet.Wallet $coin does NOT have enough money to pay $amountToPay")

class BitcoinWallet(val coldAddress: String, val hotXPrv: String) {


    val coin = "btc"

    private val blockchain = object {
        val BASE_FEE = 100L
        val FEE_PER_EXTRA_INPUT = 10L

        val rpcClient = RpcClientFactory.createBitcoinClient(
            user = config.getString("wallet.$coin.rpc.username"),
            password = config.getString("wallet.$coin.rpc.password"),
            host = config.getString("wallet.$coin.rpc.host"),
            port = config.getInt("wallet.$coin.rpc.port"),
            secure = config.getBoolean("wallet.$coin.rpc.secure")
        )
    }

    /**
     * Redis data structure for Bitcoin Wallet:
     *
     * * UTXOs                    : "btc:utxo"            : sortedSet(transactionId:vout, amount)
     * * Deposit  Transactions Id : "btc:txi:d:{addr}"    : set
     * * Overflow Transactions Id : "btc:txi:o"           : list
     * * Deposit  Transactions    : "btc:tx:d:{txid}"     : hashMap("amount", "confirmationsLeft")
     * * Withdraw Transactions    : "btc:tx:w:{txid}"     : hashMap("address", "sentAmount", "feeAmount")
     * * Archived Addresses       : "btc:addr:a"          : sortedSet(address, index)
     * * Deposit  Addresses       : "btc:addr:d"          : sortedSet(address, index)
     * * Change   Addresses       : "btc:addr:c"          : sortedSet(address, index)
     * * Cold     Addresses       : "btc:addr:o"          : list(address)
     *
     */
    private val database = object {

        private val jedis = Jedis("localhost")
        private val PREFIX = coin
        private val KEY_UTXO = "utxo"

        var balance: Int
            get() = jedis.za
            set() = {}
    }

    val cryptography = object {

        private val net = if (config.getBoolean("wallet.$coin.testnet")) Bitcoin.TEST_NET else Bitcoin.MAIN_NET

        fun derivate(index: Int, change: Int? = null) {

            val key: ExtendedPrivateKey = ExtendedPrivateKey.fromSeed(hotXPrv.toByteArray(), net)
            val childPub = key.derive("m/0'/0").neuter()


//            val base58PrivateKey =
//                "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi"
//            val extendedPrivateKey = ExtendedKeyPair.parseBase58Check(base58PrivateKey)
//
//            val serializedPublicKey = extendedPrivateKey.serializePub()
//            val serializedPrivateKey = extendedPrivateKey.serializePriv()
//
//            val masterKey = Bip32.generateMasterKey(byteArrayOf(0, 0, 0, 0))
//            val childKey = masterKey.generate("m/0/2147483647H/1")


//            val masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(HEX.decode(tv.seed))
//            assertEquals(testEncode(coldAddress), testEncode(masterPrivateKey.serializePrivB58(MAINNET)))
//            assertEquals(testEncode(tv.pub), testEncode(masterPrivateKey.serializePubB58(MAINNET)))
//            masterPrivateKey.serializePrivB58(TESTNET)
//            val dh = DeterministicHierarchy(masterPrivateKey)
//            for (i in 0 until tv.derived.size()) {
//                val tc = tv.derived.get(i)
//                log.info("{}", tc.name)
//                assertEquals(tc.name, String.format(Locale.US, "Test%d %s", testCase + 1, tc.getPathDescription()))
//                val depth = tc.path.length - 1
//                val ehkey = dh.deriveChild(Arrays.asList(tc.path).subList(0, depth), false, true, tc.path[depth])
//                assertEquals(testEncode(tc.priv), testEncode(ehkey.serializePrivB58(MAINNET)))
//                assertEquals(testEncode(tc.pub), testEncode(ehkey.serializePubB58(MAINNET)))
//            }


//            if (Crypto.signatureHmac != null) {
//                Crypto.keyHmac2.setKey(BITCOIN_SEED, 0.toShort(), BITCOIN_SEED.length as Short)
//                if (LedgerWalletApplet.proprietaryAPI != null && LedgerWalletApplet.proprietaryAPI.hasHmacSHA512()) {
//                    LedgerWalletApplet.proprietaryAPI.hmacSHA512(
//                        Crypto.keyHmac2,
//                        LedgerWalletApplet.scratch256,
//                        0.toShort(),
//                        seedLength,
//                        LedgerWalletApplet.masterDerived,
//                        0.toShort()
//                    )
//                } else {
//                    Crypto.signatureHmac.init(Crypto.keyHmac2, Signature.MODE_SIGN)
//                    Crypto.signatureHmac.sign(
//                        LedgerWalletApplet.scratch256,
//                        0.toShort(),
//                        seedLength,
//                        LedgerWalletApplet.masterDerived,
//                        0.toShort()
//                    )
//                }
//            } else {
//                HmacSha512.hmac(
//                    BITCOIN_SEED,
//                    0.toShort(),
//                    BITCOIN_SEED.length as Short,
//                    LedgerWalletApplet.scratch256,
//                    0.toShort(),
//                    seedLength,
//                    LedgerWalletApplet.masterDerived,
//                    0.toShort(),
//                    LedgerWalletApplet.scratch256,
//                    64.toShort()
//                )
//            }

//            val derive =
//                CkdFunctionDerive<Int>(0x7fffffff, arrayOfNulls(0))
//            val ketAtPath = derive.derive<Any>("m/$index${if (change!= null)  "/change" else ""}", CharSequenceDerivation.INSTANCE)
        }

        private fun sha256(messageBinary: ByteArray) = MessageDigest.getInstance("SHA-256").digest(messageBinary)

        fun calculateTransactionId(transactionHex: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.reset()
            val hash = md.digest()
            md.reset()
            return DatatypeConverter.printHexBinary(sha256(sha256(DatatypeConverter.parseHexBinary(transactionHex))))
        }
    }


    var balance: Long
        set(_) = throw Exception("You can not change the balance manually!")
        get() = database.store.computeInReadonlyTransaction { tx ->
            tx.getAll(database.TYPE_UTXO).sumByLong { it.getProperty(database.STORE_TYPE_UTXO_AMOUNT) as Long }
        }


//    /**
//     * `btc:info` is a key-value store. Here is the usage:
//     *
//     * lastUpdateTime
//     * lastIssuedIndex
//     * lastBlockHeight
//     * hotBalance
//     */
//    val infoStore = db.computeInTransaction { db.openStore("$coin:info", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:watch` is a hashMap: address ->
//     */
//    val watchStore = db.computeInTransaction { db.openStore("$coin:addr:watch", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:watch` is a hashMap: address -> {index, time, }
//     */
//    val archiveStore = db.computeInTransaction { db.openStore("$coin:addr:all", StoreConfig.WITHOUT_DUPLICATES, it) }
//
//    /**
//     * `btc:addr:utxo` is
//     */
//    val utxoStore = db.computeInTransaction { db.openStore("$coin:utxo", StoreConfig.WITHOUT_DUPLICATES, it) }

    init {
//        rpcClient.getMemoryInfo()
//        rpcClient.getBlockchainInfo()
//        rpcClient.getMempoolInfo()
//        rpcClient.getNetworkInfo()
//        rpcClient.getMiningInfo()
//        rpcClient.getPeerInfo()
//        rpcClient.getAddedNodeInfo()
//        rpcClient.getUnspentTransactionOutputSetInfo()
//        rpcClient.estimateSmartFee(1)
//        rpcClient.importMultipleAddresses(arrayListOf(AddressOrScript(object {
//            val address = "mxpejj3Wf2kvaiRUgz9CkWYwQx3HkxhiLf"
//        }, 1543599566)), ImportAddressOptions(true))
    }


    fun sendTo(address: String, amountToSend: Long): String {
        database.store.computeInExclusiveTransaction {
            val feeRate = 1 // TODO

            var estimatedFee = database.BASE_FEE * feeRate
            var inputAmount = 0L

            val outputs = mapOf(address to BigDecimal(amountToSend))
            val inputs = ArrayList<OutPoint>()

            for (utxo in it.sort(STORE_TYPE_UTXO, database.STORE_TYPE_UTXO_AMOUNT, false)) {
                inputs.add(
                    OutPoint(
                        utxo.getProperty(STORE_TYPE_UTXO_TXHASH).toString(),
                        utxo.getProperty(STORE_TYPE_UTXO_VOUT) as Int
                    )
                )
                estimatedFee += FEE_PER_EXTRA_INPUT * feeRate
                inputAmount += utxo.getProperty(STORE_TYPE_UTXO_AMOUNT) as Long
                if (inputAmount >= amountToSend + estimatedFee) break
            }

            if (inputAmount < amountToSend + estimatedFee) throw NotEnoughFundException(coin, amountToSend)

            if (inputAmount > amountToSend + estimatedFee) outputs.plus(
                Pair(
                    generateChangeAddress,
                    inputAmount - amountToSend - estimatedFee
                )
            )

            var transaction = rpcClient.createRawTransaction(inputs = inputs, outputs = outputs)
            //TODO sign
            rpcClient.sendRawTransaction(transaction)

        }




        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    suspend fun estimateFee(): Long {
//        rpcClient.est
//    }

}


abstract class Wallet(coldAddress: String, hotXPrv: String) {

    abstract val coin: String
    var isSynced = false
    abstract val rpcClient: Any

    abstract var balance: Long

    companion object {
        val all = ArrayList<Wallet>()

        fun init() {
            for (wc in config.getObject("wallet").toList()) {

                val xPrv = config.getString("wallet.${wc.first}.hotXPrv")
                val address = config.getString("wallet.${wc.first}.coldAddress")

                when (wc.first) {
                    "btc" -> all.add(BitcoinWallet(address, xPrv))
//                    "ltc" -> all.add(LitecoinWallet(address, xPrv))
//                    "eth" -> all.add(EthereumWallet(address, xPrv))
//                    "xrp" -> all.add(RippleWallet(address, xPrv))
//                    else -> throw RuntimeException("Unsupported coin ${wc.first}!!!")
                }

                logger.log(Level.INFO, "wallet.Wallet found: ${wc.first}")
            }

        }
    }


    abstract suspend fun syncBlockchain(): Unit
    abstract suspend fun subscribe(): Unit
    abstract suspend fun sendTo(address: String, amount: Long): Unit
}


object Bip32 {
    internal val curve = SECNamedCurves.getByName("secp256k1")

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @JvmOverloads
    internal fun generateMasterKey(seed: ByteArray, isMainnet: Boolean = true): ExtendedKeyPair {
        val hmac = HMac(SHA512Digest())
        val key = KeyParameter("Bitcoin seed".toByteArray())
        hmac.init(key)
        for (b in seed) {
            hmac.update(b)
        }
        val digest = ByteArray(64)
        hmac.doFinal(digest, 0)
        val l = ByteArray(32)
        val r = ByteArray(32)
        System.arraycopy(digest, 0, l, 0, 32)
        System.arraycopy(digest, 32, r, 0, 32)

        val k = parse256(l)
        return ExtendedKeyPair.Builder()
            .setPrivKey(k)
            .setChainCode(r)
            .setIsMainnet(isMainnet)
            .build()
    }

    internal fun hash160(pubKey: ECPoint): ByteArray {
        val sha256 = SHA256Digest()
        val ripemd160 = RIPEMD160Digest()

        val pubBytes = pubKey.getEncoded(true)
        sha256.update(pubBytes, 0, pubBytes.size)
        val sha256Out = ByteArray(32)
        sha256.doFinal(sha256Out, 0)
        ripemd160.update(sha256Out, 0, 32)

        val ripemdOut = ByteArray(20)
        ripemd160.doFinal(ripemdOut, 0)

        return ripemdOut
    }

    /**
     * point(p): returns the coordinate pair resulting from EC point multiplication (repeated application of the EC
     * group operation) of the secp256k1 base point with the integer p.
     */
    internal fun point(p: BigInteger): ECPoint {
        return curve.getG().multiply(p)
    }

    /**
     * ser32(i): serialize a 32-bit unsigned integer i as a 4-byte sequence, most significant byte first.
     */
    internal fun ser32(i: Int): ByteArray {
        return ByteBuffer.allocate(4).putInt(i).array()
    }

    /**
     * TODO there must be a better way
     *
     * @param p
     * @return
     */
    internal fun ser256(p: BigInteger): ByteArray {
        var p = p
        if (p.compareTo(BigInteger.ZERO) < 0) {
            val TWO_COMPL_REF = BigInteger.ONE.shiftLeft(256)
            p = p.add(TWO_COMPL_REF)
        }

        var twos = p.toByteArray()
        // BigInteger's toByteArray() gives us a big endian twos complement representation, so the leftmost byte is the
        // sign.
        val paddingNeeded = 33 - twos.size
        if (paddingNeeded > 0) {
            val newTwos = ByteArray(33)
            System.arraycopy(twos, 0, newTwos, paddingNeeded, 33 - paddingNeeded)
            twos = newTwos
        }

        val unsigned = ByteArray(32)
        System.arraycopy(twos, 1, unsigned, 0, 32)

        return unsigned
    }

    /**
     * serP(P): serializes the coordinate pair P = (x,y) as a byte sequence using SEC1's compressed form:
     * (0x02 or 0x03) || ser256(x), where the header byte depends on the parity of the omitted y coordinate.
     *
     *
     * The algorithm is specified at: http://www.secg.org/SEC1-Ver-1.0.pdf
     */
    internal fun serP(P: ECPoint): ByteArray {
        return P.getEncoded(true)
    }

    /**
     * parse256(p): interprets a 32-byte sequence as a 256-bit number, most significant byte first
     */
    internal fun parse256(p: ByteArray): BigInteger {
        return BigInteger(1, p)
    }

}

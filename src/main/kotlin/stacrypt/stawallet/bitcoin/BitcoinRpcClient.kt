package stacrypt.stawallet.bitcoin

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.googlecode.jsonrpc4j.IJsonRpcClient
import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.googlecode.jsonrpc4j.JsonRpcMethod
import com.googlecode.jsonrpc4j.ProxyUtil
import org.web3j.protocol.ObjectMapperFactory
import stacrypt.stawallet.config
import java.math.BigDecimal
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object BitcoinRpcClientFactory {

    fun createBitcoinClientWithDefaultConfig() = createBitcoinClient(
        user = config.getString("daemons.bitcoind.rpc.username"),
        password = config.getString("daemons.bitcoind.rpc.password"),
        host = config.getString("daemons.bitcoind.rpc.host"),
        port = config.getInt("daemons.bitcoind.rpc.port"),
        secure = config.getBoolean("daemons.bitcoind.rpc.secure")
    )

    @JvmStatic
    fun createBitcoinClient(
        user: String,
        password: String,
        host: String,
        port: Int,
        secure: Boolean = false,
        sslContext: SSLContext = createUnsafeSslContext()
    ):

            BitcoinRpcClient {

        val jsonRpcHttpClient: IJsonRpcClient

        jsonRpcHttpClient = JsonRpcHttpClient(
            ObjectMapperFactory.getObjectMapper().configure(
                SerializationFeature.WRITE_NULL_MAP_VALUES,
                false
            ).setSerializationInclusion(
                JsonInclude.Include.NON_NULL
            ),
            URL("${if (secure) "https" else "http"}://$user@$host:$port"),
            mapOf(Pair("Authorization", computeBasicAuth(user, password)))
        )

        jsonRpcHttpClient.setSslContext(sslContext)

        return ProxyUtil.createClientProxy(
            BitcoinRpcClientFactory::class.java.classLoader,
            BitcoinRpcClient::class.java,
            jsonRpcHttpClient
        )
    }

    private fun createUnsafeSslContext(): SSLContext {
        val dummyTrustManager = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, dummyTrustManager, SecureRandom())
        return sslContext
    }

    private fun computeBasicAuth(user: String, password: String) =
        "Basic ${BASE64.encodeToString("$user:$password".toByteArray())}"

    private val BASE64 = Base64.getEncoder()
}

interface BitcoinRpcClient {

    @JsonRpcMethod("estimatesmartfee")
    fun estimateSmartFee(
        confTarget: Int,
        estimateMode: FeeEstimateMode? = FeeEstimateMode.CONSERVATIVE
    ): EstimateSmartResult

    @JsonRpcMethod("abandontransaction")
    fun abandonTransaction(confTarget: String)

    @JsonRpcMethod("abortrescan")
    fun abortRescan()

    @JsonRpcMethod("addmultisigaddress")
    fun addMultiSigAddress(required: Int? = null, keys: List<String>): String

    @JsonRpcMethod("addnode")
    fun addNode(address: String, operation: NodeListOperation)

    @JsonRpcMethod("backupwallet")
    fun backupWallet(destination: String)

    @JsonRpcMethod("clearbanned")
    fun clearBanned()

    @JsonRpcMethod("createmultisig")
    fun createMultiSig(required: Int, keys: List<String>): MultiSigAddress

    @JsonRpcMethod("createrawtransaction")
    fun createRawTransaction(
        inputs: List<OutPoint>,
        outputs: Map<String, BigDecimal>,
        lockTime: Int? = null,
        replaceable: Boolean? = null
    ): String

    @JsonRpcMethod("decoderawtransaction")
    fun decodeRawTransaction(transactionId: String): Transaction

    @JsonRpcMethod("decodescript")
    fun decodeScript(scriptHex: String): DecodedScript

    @JsonRpcMethod("disconnectnode")
    fun disconnectNode(nodeAddress: String? = null, nodeId: Int? = null)

    @JsonRpcMethod("dumpprivkey")
    fun dumpPrivateKey(address: String): String

    @JsonRpcMethod("dumpwallet")
    fun dumpWallet(filename: String): Map<*, *>

    @JsonRpcMethod("encryptwallet")
    fun encryptWallet(passphrase: String)

    @JsonRpcMethod("generate")
    fun generate(numberOfBlocks: Int, maxTries: Int? = null): List<String>

    @JsonRpcMethod("getaddednodeinfo")
    fun getAddedNodeInfo(): List<AddedNodeInfo>

    @JsonRpcMethod("getbalance")
    fun getBalance(
        account: String = "*",
        minconf: Int = 1,
        includeWatchOnly: Boolean = false
    ): BigDecimal

    @JsonRpcMethod("getbestblockhash")
    fun getBestBlockhash(): String

    @JsonRpcMethod("getblock")
    fun getBlockData(blockHash: String, verbosity: Int = 0): String

    @JsonRpcMethod("getblock")
    fun getBlock(blockHash: String, verbosity: Int = 1): BlockInfo

    @JsonRpcMethod("getblock")
    fun getBlockWithTransactions(blockHash: String, verbosity: Int = 2): BlockInfoWithTransactions

    @JsonRpcMethod("getblockchaininfo")
    fun getBlockchainInfo(): BlockChainInfo

    @JsonRpcMethod("getblockcount")
    fun getBlockCount(): Int

    @JsonRpcMethod("getblockhash")
    fun getBlockHash(height: Int): String

    @JsonRpcMethod("getblockheader")
    fun getBlockHeader(blockHash: String, verbose: Boolean? = false): Any

    @JsonRpcMethod("getblocktemplate")
    fun getBlockTemplate(blockTemplateRequest: BlockTemplateRequest? = null)

    @JsonRpcMethod("getchaintips")
    fun getChainTips(): List<ChainTip>

    @JsonRpcMethod("getchaintxstats")
    fun getChainTransactionStats(
        blockWindowSize: Int? = null,
        blockHashEnd: String? = null
    ): ChainTransactionStats

    @JsonRpcMethod("getconnectioncount")
    fun getConnectionCount(): Int

    @JsonRpcMethod("getdifficulty")
    fun getDifficulty(): BigDecimal

    @JsonRpcMethod("getmemoryinfo")
    fun getMemoryInfo(): Any

    @JsonRpcMethod("getmempoolancestors")
    fun getMempoolAncestors(transactionId: String): Any

    @JsonRpcMethod("getmempooldescendants")
    fun getMempoolDescendants(): Any

    @JsonRpcMethod("getmempoolentry")
    fun getMempoolEntry(transactionId: String): Map<*, *>

    @JsonRpcMethod("getmempoolinfo")
    fun getMempoolInfo(): MemPoolInfo

    @JsonRpcMethod("getmininginfo")
    fun getMiningInfo(): MiningInfo

    @JsonRpcMethod("getnettotals")
    fun getNetworkTotals(): NetworkTotals

    @JsonRpcMethod("getnetworkhashps")
    fun getNetworkHashesPerSeconds(lastBlocks: Int, height: Int): Long

    @JsonRpcMethod("getnetworkinfo")
    fun getNetworkInfo(): NetworkInfo

    @JsonRpcMethod("getnewaddress")
    fun getNewAddress(): String

    @JsonRpcMethod("getpeerinfo")
    fun getPeerInfo(): List<PeerInfo>

    @JsonRpcMethod("getrawchangeaddress")
    fun getRawChangeAddress(): String

    @JsonRpcMethod("getrawmempool")
    fun getRawMemPool(verbose: Boolean = false): List<Map<*, *>>

    @JsonRpcMethod("getrawtransaction")
    fun getRawTransaction(transactionId: String): Transaction

    @JsonRpcMethod("getreceivedbyaddress")
    fun getReceivedByAddress(address: String, minConfirmations: Int = 1): BigDecimal

    @JsonRpcMethod("gettransaction")
    fun getWalletTransaction(transactionId: String): Map<*, *>

    @JsonRpcMethod("gettxout")
    fun getUnspentTransactionOutputInfo(transactionId: String, index: Int): Map<*, *>

    @JsonRpcMethod("gettxoutsetinfo")
    fun getUnspentTransactionOutputSetInfo(): UtxoSet

    @JsonRpcMethod("getwalletinfo")
    fun getWalletInfo(): Map<*, *>

    @JsonRpcMethod("importaddress")
    fun importAddress(
        scriptOrAddress: String,
        label: String? = null,
        rescan: Boolean? = null,
        includePayToScriptHash: Boolean? = null
    )

    @JsonRpcMethod("importmulti")
    fun importMultipleAddresses(
        requests: List<AddressOrScript>,
        options: ImportAddressOptions? = null
    ): List<Any>

    @JsonRpcMethod("importprivkey")
    fun importPrivateKey(
        privateKey: String,
        label: String? = null,
        rescan: Boolean? = null
    )

    @JsonRpcMethod("importpubkey")
    fun importPublicKey(
        publicKey: String,
        label: String? = null,
        rescan: Boolean? = null
    )

    @JsonRpcMethod("importwallet")
    fun importWallet(walletFile: String)

    @JsonRpcMethod("keypoolrefill")
    fun keypoolRefill(newSize: Int = 100)

    @JsonRpcMethod("listaddressgroupings")
    fun listAddressGroupings(): List<*>

    @JsonRpcMethod("listbanned")
    fun listBanned(): List<String>

    @JsonRpcMethod("listlockunspent")
    fun listLockUnspent(): List<Map<*, *>>

    @JsonRpcMethod("listreceivedbyaddress")
    fun listReceivedByAddress(
        minConfirmations: Int? = null,
        includeEmpty: Boolean? = null,
        includeWatchOnly: Boolean? = null
    ): List<Map<*, *>>

    @JsonRpcMethod("listsinceblock")
    fun listSinceBlock(
        blockHash: String? = null,
        targetConfirmations: Int? = null,
        includeWatchOnly: Boolean? = null,
        includeRemoved: Boolean? = null
    ): Map<*, *>

    @JsonRpcMethod("listtransactions")
    fun listTransactions(
        account: String? = null,
        count: Int? = null,
        skip: Int? = null,
        includeWatchOnly: Boolean? = null
    ): List<Map<*, *>>

    @JsonRpcMethod("listunspent")
    fun listUnspent(
        minConfirmations: Int? = null,
        maxConfirmations: Int? = null,
        addresses: List<String>? = null,
        includeUnsafe: Boolean? = null,
        queryOptions: QueryOptions? = null
    ): QueryResult

    @JsonRpcMethod("listwallets")
    fun listWallets(): List<String>

    @JsonRpcMethod("lockunspent")
    fun lockUnspent(unlock: Boolean, unspentOutputs: List<OutPoint>): Boolean

    fun ping()

    @JsonRpcMethod("preciousblock")
    fun preciousBlock(block: String)

    @JsonRpcMethod("prioritisetransaction")
    fun prioritiseTransaction(transactionId: String, dummy: Int, feeDeltaSatoshis: Int)

    @JsonRpcMethod("pruneblockchain")
    fun pruneBlockchain(blockHeightOrUnixTimestamp: Long)

    @JsonRpcMethod("removeprunedfunds")
    fun removePrunedFunds(transactionId: String)

    @JsonRpcMethod("sendmany")
    fun sendMany(
        account: String,
        addressAmounts: Map<String, BigDecimal>,
        comment: String? = null,
        subtractFee: Boolean = false,
        replaceable: Boolean = false,
        minConfirmations: Int? = null,
        feeEstimateMode: FeeEstimateMode? = null
    )

    @JsonRpcMethod("sendrawtransaction")
    fun sendRawTransaction(transaction: String): String

    @JsonRpcMethod("sendtoaddress")
    fun sendToAddress(
        address: String,
        amount: BigDecimal,
        comment: String? = null,
        commentTo: String? = null,
        subtractFee: Boolean? = null,
        replaceable: Boolean? = null,
        minConfirmations: Int? = null,
        feeEstimateMode: FeeEstimateMode? = null
    ): String

    @JsonRpcMethod("setban")
    fun setBan(
        address: String,
        operation: NodeListOperation,
        seconds: Int
    )

    @JsonRpcMethod("settxfee")
    fun setTransactionFee(fee: Double)

    @JsonRpcMethod("signmessage")
    fun signMessage(
        address: String,
        message: String
    )

    @JsonRpcMethod("signmessagewithprivkey")
    fun signMessageWithPrivateKey(
        privateKey: String,
        message: String
    )

    @JsonRpcMethod("signrawtransaction")
    fun signRawTransaction(transactionId: String)

    @JsonRpcMethod("signrawtransactionwithkey")
    fun signRawTransactionWithKey(hexString: String, privKeys: List<String>): SignTransactionResult

    @JsonRpcMethod("submitblock")
    fun submitBlock(blockData: String)

    fun uptime(): Int

    @JsonRpcMethod("validateaddress")
    fun validateAddress(address: String)

    @JsonRpcMethod("verifychain")
    fun verifyChain()

    @JsonRpcMethod("verifymessage")
    fun verifyMessage(
        address: String,
        signature: String,
        message: String
    )

    @JsonRpcMethod("searchrawtransactions")
    fun searchRawSerialisedTransactions(
        address: String,
        verbose: Int? = 0,
        skip: Int? = null,
        count: Int? = null,
        vInExtra: Int? = null,
        reverse: Boolean? = null
    ): List<String>

    @JsonRpcMethod("searchrawtransactions")
    fun searchRawVerboseTransactions(
        address: String,
        verbose: Int? = 1,
        skip: Int? = null,
        count: Int? = null,
        vInExtra: Int? = null,
        reverse: Boolean? = null
    ): List<SearchedTransactionResult>

    /**
     * btcd-specific extension methods
     */
    @JsonRpcMethod("authenticate")
    fun btcdAuthenticate(username: String, password: String)

    @JsonRpcMethod("generate")
    fun btcdGenerate(numberOfBlocks: Int): List<String>

    @JsonRpcMethod("getblock")
    fun btcdGetBlockWithTransactions(blockHash: String, verbose: Boolean = true): String
}


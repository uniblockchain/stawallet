package stacrypt.stawallet.ripple

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.googlecode.jsonrpc4j.*
import org.web3j.protocol.ObjectMapperFactory
import stacrypt.stawallet.config
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RippleRpcClientFactory {
    fun createRippleClientWithDefaultConfig() = RippleRpcClientFactory.createRippleClient(
        user = config.getString("daemons.rippled.rpc.username"),
        password = config.getString("daemons.rippled.rpc.password"),
        host = config.getString("daemons.rippled.rpc.host"),
        port = config.getInt("daemons.rippled.rpc.port"),
        secure = config.getBoolean("daemons.rippled.rpc.secure")
    )

    @JvmStatic
    fun createRippleClient(
        user: String,
        password: String,
        host: String,
        port: Int,
        secure: Boolean = false,
        sslContext: SSLContext = createUnsafeSslContext()
    ):

            RippleRpcClient {

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
            RippleRpcClientFactory::class.java.classLoader,
            RippleRpcClient::class.java,
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

interface RippleRpcClient {
    @JsonRpcMethod("account_info")
    fun getAccountInfo(
        @JsonRpcParam("account") account: String,
        @JsonRpcParam("ledger_index") ledgerIndex: Any? = "validated"
    ): GetAccountResponse

    @JsonRpcMethod("tx")
    fun getTransaction(
        @JsonRpcParam("transaction") transactionHex: String,
        @JsonRpcParam("binary") binary: Boolean = false
    ): GetTxInfoResponse

    /**
     * @start: Number of transactions to skip over.
     */
    @JsonRpcMethod("tx_history")
    fun getTransactionHistory(
        @JsonRpcParam("transaction") start: Int
    ): GetTxHistoryResponse

    @JsonRpcMethod("fee")
    fun fee(): FeeResult

    @JsonRpcMethod("submit")
    fun submitTransaction(
        @JsonRpcParam("tx_blob") transactionHex: String
    ): SubmitTxResponse

    @JsonRpcMethod("sign")
    fun signTransaction(
        @JsonRpcParam("tx_json") transaction: Transaction,
        @JsonRpcParam("seed") seed: String,
        @JsonRpcParam("key_type") keyType: String = "secp256k1",
        @JsonRpcParam("fee_mult_max") feeMultMax: Int = 1_000,
        @JsonRpcParam("fee_div_max") feeDivMax: Int = 1
    ): SignTxResponse
}
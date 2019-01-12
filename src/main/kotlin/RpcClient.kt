import bitcoin.BitcoinRpcClient
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.googlecode.jsonrpc4j.IJsonRpcClient
import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.googlecode.jsonrpc4j.ProxyUtil
import org.web3j.protocol.ObjectMapperFactory
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RpcClientFactory {

    @JvmStatic
    fun createBitcoinClient(daemon: String) = createBitcoinClient(
        user = config.getString("daemons.$daemon.rpc.username"),
        password = config.getString("daemons.$daemon.rpc.password"),
        host = config.getString("daemons.$daemon.rpc.host"),
        port = config.getInt("daemons.$daemon.rpc.port"),
        secure = config.getBoolean("daemons.$daemon.rpc.secure")
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
            RpcClientFactory::class.java.classLoader,
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

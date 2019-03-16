package stacrypt.stawallet

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.bitcoin.BitcoinWallet
import stacrypt.stawallet.model.*
import stacrypt.stawallet.rest.walletsRouting
import java.net.URI
import java.sql.Connection
import java.util.logging.Level
import java.util.logging.Logger


val wallets: List<Wallet> by lazy { Wallet.initFromConfig() }
private val logger = Logger.getLogger("Application")

//fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

fun watch(walletName: String) = wallets.findLast { it.name == walletName }!!.startBlockchainWatcher()

@UseExperimental(KtorExperimentalAPI::class)
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    logger.log(Level.INFO, "Loading config...")
    config = (environment.config as HoconApplicationConfig).getTypesafeConfig()

    logger.log(Level.WARNING, "Initializing wallets:")


    logger.log(Level.WARNING, "Syncing wallets:")

    install(AutoHeadResponse)

    install(CallLogging) {
        level = org.slf4j.event.Level.TRACE
//        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    connectToDatabase()

    routing {
        get("/") {
            return@get call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        walletsRouting()

    }


    // FIXME
    if (!testing) {
        initBaseData(true) // FIXME: Development-only

    }
}


// FIXME This function is ONLY for DEVELOPMENT purposes and should not be used in production.
fun initBaseData(force: Boolean = false) {
}

fun connectToDatabase() {
    val connectionStringUri =
        URI.create(config.getString("db.uri").replace("postgres://", "postgresql://"))
    Database.connect(
        "jdbc:${connectionStringUri.scheme}://${connectionStringUri.host}:${connectionStringUri.port}${connectionStringUri.path}",
        user = connectionStringUri.userInfo.split(":")[0],
        password = connectionStringUri.userInfo.split(":")[1],
        driver = "org.postgresql.Driver",
        setupConnection = { connection: Connection -> connection.autoCommit = false; }
    )
}

fun HoconApplicationConfig.getTypesafeConfig(): Config {
    return javaClass.getDeclaredField("config").let {
        it.isAccessible = true
        return@let it[this] as Config
    }
}


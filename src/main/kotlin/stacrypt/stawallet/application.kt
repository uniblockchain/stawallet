package stacrypt.stawallet

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
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
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.bitcoin.BitcoinWallet
import stacrypt.stawallet.model.*
import stacrypt.stawallet.rest.walletsRouting
import java.net.URI
import java.sql.Connection
import java.util.logging.Level
import java.util.logging.Logger


lateinit var config: Config
lateinit var wallets: List<Wallet>
private val logger = Logger.getLogger("Application")

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)


@UseExperimental(KtorExperimentalAPI::class)
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    logger.log(Level.INFO, "Loading config...")
    config = (environment.config as HoconApplicationConfig).getTypesafeConfig()

    logger.log(Level.WARNING, "Initializing wallets:")

    wallets = Wallet.initFromConfig()

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

    initDatabase()

    routing {
        get("/") {
            return@get call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        walletsRouting()

    }


    // FIXME
    if (!testing) {
        initBaseData(true) // FIXME: Development-only
        initWatchers()
    }
}

fun initWatchers() {
    wallets.forEach {
        // FIXME
        if (it is BitcoinWallet) it.startBlockchainWatcher()
    }

}

// FIXME This function is ONLY for DEVELOPMENT purposes and should not be used in production.
fun initBaseData(force: Boolean = false) {
    wallets.forEach { wallet ->
        try {
            transaction {
                if (wallet is BitcoinWallet) {
                    if (WalletDao.findById("btc") == null) {
                        WalletDao.new("btc") {
                            blockchain = BlockchainDao.new {
                                this.currency = "tbtc"
                                this.network = "testnet3"
                            }
                            seedFingerprint = "" // FIXME
                            path = "m/44'/1'/0'"
                            latestSyncedHeight = 1_484_780
                        }
                    } else {
                        WalletDao["btc"].latestSyncedHeight = 1_484_780
                    }
                }
            }
        } catch (e: Exception) {
            println("It seems the ${wallet.name} wallet is already exists in database (or could not be added).")
        }
    }
}

@KtorExperimentalAPI
fun initDatabase(dropIfExists: Boolean = false) {

    val connectionStringUri =
        URI.create(config.getString("db.uri").replace("postgres://", "postgresql://"))
    Database.connect(
        "jdbc:${connectionStringUri.scheme}://${connectionStringUri.host}:${connectionStringUri.port}${connectionStringUri.path}",
        user = connectionStringUri.userInfo.split(":")[0],
        password = connectionStringUri.userInfo.split(":")[1],
        driver = "org.postgresql.Driver",
        setupConnection = { connection: Connection -> connection.autoCommit = false; }
    )

    val tables = arrayOf(WalletTable, AddressTable, InvoiceTable, DepositTable, ProofTable, TaskTable, UtxoTable)

    transaction {
        if (dropIfExists) {
            SchemaUtils.drop(*tables)
            flushCache()
        }
        SchemaUtils.create(*tables)
        commit()
    }
}

fun HoconApplicationConfig.getTypesafeConfig(): Config {
    return javaClass.getDeclaredField("config").let {
        it.isAccessible = true
        return@let it[this] as Config
    }
}


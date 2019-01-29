package stacrypt.stawallet

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.*
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.config.ApplicationConfig
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.request.path
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
//import jetbrains.exodus.env.Environments
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import redis.clients.jedis.Jedis
import stacrypt.stawallet.model.*
import stacrypt.stawallet.rest.walletsRouting
import java.net.URI
import java.sql.Connection
import java.util.logging.*


val config: Config = ConfigFactory.load()
lateinit var applicationConfig: ApplicationConfig
//val jedis = Jedis()
lateinit var wallets : List<Wallet>
private val logger = Logger.getLogger("Application")
//val db = Environments.newInstance(stacrypt.stawallet.getConfig.getString("db.envPath"))
//lateinit var restApplicationJob: Job

fun main(args: Array<String>) {


//    val store = db.computeInTransaction { db.openStore("ltc", StoreConfig.WITHOUT_DUPLICATES, it) }

//    db.executeInTransaction { store.put(it, stringToEntry("Hello"), stringToEntry("World!")) }

//    println(db.computeInTransaction { entryToString(store.get(it, stringToEntry("Hello"))!!) })

    logger.log(Level.WARNING, "Initializing wallets:")

    wallets = Wallet.initFromConfig()

    logger.log(Level.WARNING, "Syncing wallets:")

    wallets.forEach {
        runBlocking {
            // TODO: Sync coins
        }
    }

    return io.ktor.server.netty.EngineMain.main(args)

//    db.close()
}

@UseExperimental(KtorExperimentalAPI::class)
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    applicationConfig = environment.config

    install(AutoHeadResponse)

    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
        filter { call -> call.request.path().startsWith("/") }
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
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }
        walletsRouting()
    }
}

@KtorExperimentalAPI
fun initDatabase(dropIfExists: Boolean = false) {

    val connectionStringUri =
        URI.create(applicationConfig.property("db.uri").getString().replace("postgres://", "postgresql://"))
    Database.connect(
        "jdbc:${connectionStringUri.scheme}://${connectionStringUri.host}:${connectionStringUri.port}${connectionStringUri.path}",
        user = connectionStringUri.userInfo.split(":")[0],
        password = connectionStringUri.userInfo.split(":")[1],
        driver = "org.postgresql.Driver",
        setupConnection = { connection: Connection -> connection.autoCommit = false; }
    )

    val tables = arrayOf(WalletTable, AddressTable, EventTable, TaskTable, UtxoTable)

    transaction {
        if (dropIfExists) {
            SchemaUtils.drop(*tables)
            flushCache()
        }
        SchemaUtils.create(*tables)
        commit()
    }
}


inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

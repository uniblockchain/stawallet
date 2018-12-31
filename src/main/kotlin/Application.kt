import com.typesafe.config.*
import jetbrains.exodus.entitystore.StoreTransaction
import jetbrains.exodus.entitystore.StoreTransactionalExecutable
//import jetbrains.exodus.env.Environments
import jetbrains.exodus.env.Store
import kotlinx.coroutines.*
import redis.clients.jedis.Jedis
import java.util.logging.*
import jetbrains.exodus.env.StoreConfig
import jetbrains.exodus.env.TransactionalComputable
import org.jetbrains.annotations.NotNull
import jetbrains.exodus.bindings.StringBinding.stringToEntry
import jetbrains.exodus.bindings.StringBinding.entryToString


val config: Config = ConfigFactory.load()
val jedis = Jedis()
private val logger = Logger.getLogger("Application")
//val db = Environments.newInstance(config.getString("db.envPath"))


fun main(args: Array<String>) {

//    val store = db.computeInTransaction { db.openStore("ltc", StoreConfig.WITHOUT_DUPLICATES, it) }

//    db.executeInTransaction { store.put(it, stringToEntry("Hello"), stringToEntry("World!")) }

//    println(db.computeInTransaction { entryToString(store.get(it, stringToEntry("Hello"))!!) })

    logger.log(Level.WARNING, "Initializing wallets:")

    Wallet.init()

    logger.log(Level.WARNING, "Syncing wallets:")

    Wallet.all.forEach {
        runBlocking {
            // TODO: Sync wallet
        }
    }


//    db.close()
}


inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

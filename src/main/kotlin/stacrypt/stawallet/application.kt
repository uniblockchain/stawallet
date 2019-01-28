package stacrypt.stawallet

import com.typesafe.config.*
//import jetbrains.exodus.env.Environments
import kotlinx.coroutines.*
import redis.clients.jedis.Jedis
import java.util.logging.*


val config: Config = ConfigFactory.load()
val jedis = Jedis()
private val logger = Logger.getLogger("Application")
//val db = Environments.newInstance(stacrypt.stawallet.getConfig.getString("db.envPath"))


fun main(args: Array<String>) {

//    val store = db.computeInTransaction { db.openStore("ltc", StoreConfig.WITHOUT_DUPLICATES, it) }

//    db.executeInTransaction { store.put(it, stringToEntry("Hello"), stringToEntry("World!")) }

//    println(db.computeInTransaction { entryToString(store.get(it, stringToEntry("Hello"))!!) })

    logger.log(Level.WARNING, "Initializing wallets:")

    val wallets = Wallet.initFromConfig()

    logger.log(Level.WARNING, "Syncing wallets:")

    wallets.forEach {
        runBlocking {
            // TODO: Sync coins
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

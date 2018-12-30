import com.typesafe.config.*
import kotlinx.coroutines.*
import redis.clients.jedis.Jedis
import java.util.logging.*

val config: Config = ConfigFactory.load()
val jedis = Jedis()
private val logger = Logger.getLogger("Application")


fun main(args: Array<String>) {
    logger.log(Level.WARNING, "Initializing wallets:")

    Wallet.init()

    logger.log(Level.WARNING, "Syncing wallets:")

    Wallet.all.forEach {
        runBlocking {
            // TODO: Sync wallet
        }
    }

}

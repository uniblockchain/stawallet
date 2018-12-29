import com.typesafe.config.*
import kotlinx.coroutines.*
import java.util.logging.*


val config: Config = ConfigFactory.load()
val logger: Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)


fun main(args: Array<String>) {
    logger.log(Level.INFO, "Initializing wallets:")

    println("Start")

// Start a coroutine
    GlobalScope.launch {
        delay(1000)
        println("Hello")
    }

//    Thread.sleep(2000) // wait for 2 seconds
    println("Stop")
    Wallet.init()

    for (w in Wallet.all){
        print("salam")
    }

}

//fun initializeWallets(): List<CryptocurrencyWallet>{
//    for (wc in config.getList("wallet")){
//
//    }
//}
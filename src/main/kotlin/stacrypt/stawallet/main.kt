package stacrypt.stawallet

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.bitcoin.BitcoinWallet
import stacrypt.stawallet.ethereum.EthereumWallet
import stacrypt.stawallet.model.*
import java.net.URI


var config: Config = ConfigFactory.load()

private object cli {
    class Create : CliktCommand("Create database") {
        val force: Boolean by option("--force", help = "Drop existing database").flag()
        override fun run() {
            val connectionStringUri =
                URI.create(config.getString("db.uri").replace("postgres://", "postgresql://"))
            val databaseName = connectionStringUri.path.replace("/", "")
            val user = connectionStringUri.userInfo.split(":")[0]

            val statement = """
                ${if (force) "DROP DATABASE $databaseName;" else ""}
                CREATE DATABASE $databaseName;
                GRANT ALL PRIVILEGES ON $databaseName TO $user;
            """.trimIndent()

            echo("Execute the printed statements to your sql database by 'root' user:", err = true)
            echo(statement)
        }
    }

    class Init : CliktCommand("Setup database schema") {
        val force: Boolean by option(help = "Clear existing schema").flag("--force")
        override fun run() {
            connectToDatabase()
            val tables =
                arrayOf(WalletTable, AddressTable, InvoiceTable, DepositTable, ProofTable, TaskTable, UtxoTable)
            transaction {
                if (force) {
                    SchemaUtils.drop(*tables)
                    flushCache()
                }
                SchemaUtils.create(*tables)
                commit()
            }

        }
    }

    class Populate : CliktCommand("Populate database with config's base data") {
        val force: Boolean by option(help = "Override existing data").flag("--force")
        override fun run() {
            transaction {
                wallets.forEach { wallet ->
                    try {
                        transaction { wallet.initializeToDb(force) }
                    } catch (e: Exception) {
                        println("It seems the ${wallet.name} wallet is already exists in database (or could not be added).")
                    }
                }
            }
        }
    }

    class Migrate : CliktCommand("Database migration") {
        override fun run() {
            TODO()
        }
    }

    class Database : CliktCommand("Database administration") {
        override fun run() = Unit
    }

    class Watch : CliktCommand("Start blockchain watcher") {
        val walletName: String by option(help = "Wallet name").required()
        override fun run() {
            watch(walletName)
        }
    }

    class Serve : CliktCommand("Serve the rest api server") {
        override fun run() = io.ktor.server.netty.EngineMain.main(emptyArray())
    }

    class Stawallet : CliktCommand("Stawallet command line interface") {
        override fun run() = Unit
    }
}

fun main(args: Array<String>) = cli.Stawallet()
    .subcommands(
        cli.Database().subcommands(cli.Create(), cli.Init(), cli.Populate(), cli.Migrate()),
        cli.Serve(),
        cli.Watch()
    )
    .main(args)

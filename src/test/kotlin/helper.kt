package stacrypt.stawallet

import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withApplication
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.slf4j.LoggerFactory
import stacrypt.stawallet.module

@KtorExperimentalAPI
abstract class BaseApiTest {

    @get:Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    @get:Rule
    var tpr = TestApplicationRule()

    var testEngine: TestApplicationEngine? = null

    @Ignore
    open fun configure(): MutableMap<String, String> = mutableMapOf(
        "ktor.deployment.environment" to "test",
        "db.uri" to "postgresql://postgres:postgres@localhost:${pg.embeddedPostgres.port}/postgres"
    ).apply {
        put("daemons.bitcoind.rpc.username", "")
        put("daemons.bitcoind.rpc.password", "")
        put("daemons.bitcoind.rpc.host", "")
        put("daemons.bitcoind.rpc.port", "0")
        put("daemons.bitcoind.rpc.secure", "false")
        put("daemons.bitcoind.zmq.host", "")
        put("daemons.bitcoind.zmq.port", "0")
    }

    @Ignore
    open fun mockup(app: Application) {
    }

    inner class TestApplicationRule : TestRule {
        override fun apply(base: Statement?, description: Description?): Statement {
            return object : Statement() {
                @Throws(Throwable::class)
                override fun evaluate() {
                    withApplication(applicationEngineEnvironment {
                        config = HoconApplicationConfig(ConfigFactory.parseMap(configure()))
                        log = LoggerFactory.getLogger("ktor.test")
                    })
                    {
                        application.module(testing = true)
                        mockup(application)
                        testEngine = this
                        base?.evaluate()
                    }

                }
            }
        }
    }

}

fun String.toJson() = ObjectMapper().readTree(this)
package com.perfect.apartmentrental

import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import stacrypt.stawallet.module

@KtorExperimentalAPI
abstract class BaseApiTest {

    @get:Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    @get:Rule
    var tpr = TestApplicationRule()

    var testEngine: TestApplicationEngine? = null

    @Ignore
    open fun config(app: Application) {
        // TODO: Load application.conf file
        (app.environment.config as MapApplicationConfig).apply {
            put("db.uri", "postgresql://postgres:postgres@localhost:${pg.embeddedPostgres.port}/postgres")
        }
    }

    @Ignore
    open fun mockup(app: Application) {
    }


    inner class TestApplicationRule : TestRule {
        override fun apply(base: Statement?, description: Description?): Statement {
            return object : Statement() {
                @Throws(Throwable::class)
                override fun evaluate() {
                    withTestApplication({
                        config(this)
                        module(testing = true)
                        mockup(this)
                    }) {
                        testEngine = this
                        base?.evaluate()
                    }
                }
            }
        }
    }


}

fun String.toJson() = ObjectMapper().readTree(this)
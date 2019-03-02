package stacrypt.stawallet.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.util.pipeline.ContextDsl
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import org.jetbrains.exposed.sql.transactions.experimental.transaction
import stacrypt.stawallet.Wallet
import stacrypt.stawallet.wallets
import java.lang.Exception

inline val PipelineContext<*, ApplicationCall>.wallet: Wallet
    get() = wallets.findLast { it.name == call.parameters["wallet"].toString() }!!

fun PipelineContext<*, ApplicationCall>.qs(key: String): String? = call.request.queryParameters[key]

/**
 * Let's define an easy way to access some usual query strings with the same use cases.
 *
 * NOTE: Don't use these value instead of form parameters in post requests!
 */
inline val PipelineContext<*, ApplicationCall>.page: Int
    get() = qs("page")?.toInt() ?: 0

inline val PipelineContext<*, ApplicationCall>.user: String?
    get() = qs("user")

inline val PipelineContext<*, ApplicationCall>.force: Boolean
    get() = qs("force")?.toBoolean() ?: false

/**
 * Safe + Transactional + Lambda-Response
 */
@ContextDsl
fun Route.reachGet(path: String, body: PipelineInterceptor<Unit, ApplicationCall>): Route =
    get(path) {
        try {
            transaction {
                body.invoke(this@get, Unit)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }
    }
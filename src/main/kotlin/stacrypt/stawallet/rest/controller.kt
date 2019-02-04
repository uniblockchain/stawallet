package stacrypt.stawallet.rest

import io.ktor.application.call
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.transaction
import stacrypt.stawallet.model.InvoiceDao
import stacrypt.stawallet.model.InvoiceTable
import stacrypt.stawallet.model.WalletDao
import stacrypt.stawallet.wallets
import java.lang.Exception

fun Routing.walletsRouting() {
    route("/wallets") {
        get {
            transaction {
                call.respond(WalletDao.all().toList().map { it.export() })
            }
        }

        route("/{walletId}") {
            get("") {
                transaction {
                    call.respond(WalletDao[call.parameters["walletId"].toString()].export())
                }
            }
            invoicesRout()
            depositsRout()
            withdrawsRout()
        }

    }
}


fun Route.invoicesRout() = route("/invoices") {
    contentType(FormUrlEncoded) {
        /**
         * This service will generate a invoice for the user.
         *
         * The `force` parameter is true, it will create a new invoice for the user. Otherwise the service will response
         * a 409 status code which means that there is at least one active and unused invoice for this user. You could
         */
        post {
            val form: Parameters = call.receiveParameters()
            val user = form["user"]!!
            val force = call.request.queryParameters["force"]?.toBoolean() ?: false

            try {
                transaction {
                    val wallet = wallets.findLast { it.name == call.parameters["walletId"] }!!
                    val lastUsableInvoice = wallet.lastUsableInvoice(user)
                    flushCache()

                    if (force || lastUsableInvoice == null || wallet.invoiceDeposits(lastUsableInvoice.id.value).isNotEmpty()) {
                        call.respond(wallet.issueNewInvoice(user).export())
                    } else {
                        call.respond(
                            HttpStatusCode.Conflict,
                            "There is at least one active and unused invoice for this user"
                        )
                    }
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.toString())
            }
        }
    }

    /**
     * Give us the invoice list of the mentioned user.
     * If there is not any invoice for this user, this Api will return an empty list.
     * If the result was empty or there ws not any active invoice fot the user, you should call Post method to request a
     * new invoice for this user.
     * 404 Not Found exception. So you should try to post an invoice first.
     */
    get {
        val user = call.request.queryParameters["user"]!!.toString()

        try {
            transaction {
                val wallet = wallets.findLast { it.name == call.parameters["walletId"].toString() }!!

                call.respond(
                    InvoiceDao.wrapRows(InvoiceTable.select {
                        (InvoiceTable.wallet eq wallet.name) and (InvoiceTable.user eq user)
                    }.orderBy(InvoiceTable.creation, false)).toList().map { it.export() }
                )
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }

    }

    get("{invoiceId}") {
        val user = call.request.queryParameters["user"]!!.toString()

        try {
            transaction {
                val wallet = wallets.findLast { it.name == call.parameters["walletId"].toString() }!!
                val invoice = InvoiceDao[call.parameters["invoiceId"]!!.toInt()]
                if (invoice.wallet.id.value != wallet.name) call.respond(HttpStatusCode.NotFound)
                call.respond(invoice.export())
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }

    }

}

fun Route.depositsRout() = route("/deposits") {

}

fun Route.withdrawsRout() = route("/withdraws") {

}
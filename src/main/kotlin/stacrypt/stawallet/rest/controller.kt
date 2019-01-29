package stacrypt.stawallet.rest

import io.ktor.application.call
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.Parameters
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.model.InvoicePurpose
import stacrypt.stawallet.model.WalletDao

fun Routing.walletsRouting() {
    route("/wallets") {
        get {
            return@get call.respond(transaction { WalletDao.all().toList() }.map { it.export() })
        }

        route("/{id}") {
            get("") {
                return@get call.respond(transaction { WalletDao[call.parameters["id"].toString()] }.export())
            }
            invoicesRout()
            depositsRout()
            withdrawsRout()
        }

    }
}


fun Route.invoicesRout() = route("/invoices") {
    contentType(FormUrlEncoded) {
        post {
            val form: Parameters = call.receive()
            val purpose = InvoicePurpose.valueOf(form["purpose"]!!.toUpperCase())
            transaction {

            }
        }
    }
}

fun Route.depositsRout() = route("/deposits") {

}

fun Route.withdrawsRout() = route("/withdraws") {

}
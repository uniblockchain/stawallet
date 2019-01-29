package stacrypt.stawallet.rest

import io.ktor.application.call
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.Parameters
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import org.jetbrains.exposed.sql.stringParam
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.model.InvoicePurpose
import stacrypt.stawallet.model.WalletDao
import stacrypt.stawallet.wallets
import java.lang.Exception

fun Routing.walletsRouting() {
    route("/wallets") {
        get {
            return@get call.respond(transaction { WalletDao.all().toList() }.map { it.export() })
        }

        route("/{wallet}") {
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
            val form: Parameters = call.receiveParameters()
            val user = form["user"]!!
            val force = form["force"]!!
            val purpose = InvoicePurpose.valueOf(form["purpose"]!!.toUpperCase())
            try {
                val wallet = wallets.findLast { it.name == call.parameters["wallet"] }!!
                
                val lastIssuedInvoice = wallet.
                wallet.issueInvoice(user, purpose)
            }catch(e: Exception){

            }
        }
    }
}

fun Route.depositsRout() = route("/deposits") {

}

fun Route.withdrawsRout() = route("/withdraws") {

}
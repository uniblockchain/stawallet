package stacrypt.stawallet.rest

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import stacrypt.stawallet.model.Wallet

fun Routing.walletsRouting() {
    route("/wallets") {
        get("") {
            return@get call.respond(transaction { Wallet.all() }.map { it.export() })
        }

        route("/{id}") {
            get("") {
                return@get call.respond(transaction { Wallet[call.parameters["id"].toString()] }.export())
            }
            addressesRout()
            depositsRout()
            withdrawsRout()
        }

    }
}


fun Route.addressesRout() = route("/addresses") {

}

fun Route.depositsRout() = route("/deposits") {

}

fun Route.withdrawsRout() = route("/withdraws") {

}
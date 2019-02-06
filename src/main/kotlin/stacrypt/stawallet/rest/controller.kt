package stacrypt.stawallet.rest

import io.ktor.application.call
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.transaction
import org.joda.time.DateTime
import stacrypt.stawallet.model.*
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

    get("/{invoiceId}") {
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
    get {
        val user = call.request.queryParameters["user"]
        val isAccepted = call.request.queryParameters["isAccepted"]?.toBoolean() ?: true
        val page = call.request.queryParameters["page"]?.toInt() ?: 0

        try {
            transaction {
                val wallet = wallets.findLast { it.name == call.parameters["wallet"].toString() }!!

                if (isAccepted)
                    call.respond(
                        DepositDao.wrapRows(
                            DepositTable.leftJoin(InvoiceTable)
                                .select { InvoiceTable.wallet eq wallet.name }
                                .andWhere { InvoiceTable.user eq user }
                                .orderBy(DepositTable.id)
                                .limit(DepositResource.PAGE_SIZE, DepositResource.PAGE_SIZE * page)
                        ).forEach { it.export(null, wallet) }
                    )
                else
                    call.respond(
                        ProofDao.wrapRows(
                            ProofTable.leftJoin(InvoiceTable)
                                .select { InvoiceTable.wallet eq wallet.name }
                                .andWhere { InvoiceTable.user eq user }
                                .orderBy(ProofTable.id)
                                .limit(ProofResource.PAGE_SIZE, ProofResource.PAGE_SIZE * page)
                        ).forEach { it.exportAsDeposit(null, wallet) }
                    )

            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }
    }
}

fun Route.withdrawsRout() = route("/withdraws") {
    get {
        val user = call.request.queryParameters["user"]
        val page = call.request.queryParameters["page"]?.toInt() ?: 0

        try {
            transaction {
                val wallet = wallets.findLast { it.name == call.parameters["wallet"].toString() }!!

                call.respond(
                    TaskDao.wrapRows(
                        TaskTable
                            .select { TaskTable.wallet eq wallet.name }
                            .run { if (user != null) this.andWhere { TaskTable.user eq user } else this }
                            .orderBy(TaskTable.id)
                            .limit(WithdrawResource.PAGE_SIZE, WithdrawResource.PAGE_SIZE * page)
                    ).forEach { it.export(null, wallet) }
                )

            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }
    }

    contentType(FormUrlEncoded) {
        post {
            val form: Parameters = call.receiveParameters()
            val user = form["user"]!!
            val businessUid = form["businessUid"]!!
            val isManual = form["isManual"]!!.toBoolean()
            val target = form["target"]!!
            val netAmount = form["netAmount"]!!.toLong()
            val grossAmount = form["grossAmount"]!!.toLong()
            val estimatedNetworkFee = form["estimatedNetworkFee"]!!.toLong()
            val type = TaskType.valueOf(form["type"]!!.toLowerCase())

            try {
                transaction {
                    val taskWithSameBusinessId = TaskTable.select { TaskTable.businessUid eq businessUid }.firstOrNull()
                    if (taskWithSameBusinessId != null)
                        call.respond(
                            HttpStatusCode.Conflict,
                            "This business id has been already used by withdraw with id:$taskWithSameBusinessId"
                        )
                    else
                        call.respond(
                            TaskDao.new {
                                this.wallet = WalletDao[call.parameters["walletId"]!!]
                                this.businessUid = businessUid
                                this.user = user
                                this.target = target
                                this.netAmount = netAmount
                                this.grossAmount = grossAmount
                                this.estimatedNetworkFee = estimatedNetworkFee
                                this.type = type
                                this.status = if (isManual) TaskStatus.WAITING_MANUAL else TaskStatus.QUEUED
                                this.trace = "${DateTime.now()} : Issued (${if (isManual) "manual" else "automatic"})"
                            }
                        )
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.toString())
            }
        }

        put("/{id}") {
            val form: Parameters = call.receiveParameters()
            val isManual = form["isManual"]?.toBoolean()
            val finalNetworkFee = form["finalNetworkFee"]?.toLong()
            val txid = form["txid"]

            try {
                transaction {
                    val task = TaskDao.findById(call.parameters["id"]!!.toInt())
                    if (task == null || task.wallet.id.value != call.parameters["walletId"]!!) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Withdraw record not found"
                        )
                    } else {
                        if (isManual == true && arrayOf(
                                TaskStatus.QUEUED,
                                TaskStatus.ERROR,
                                TaskStatus.WAITING_LOW_BALANCE
                            ).contains(task.status)
                        ) {
                            task.status = TaskStatus.WAITING_MANUAL
                            task.trace = task.trace + "\n${DateTime.now()} : Change to manual"
                        } else if (isManual == false && task.status == TaskStatus.WAITING_MANUAL) {
                            task.status = TaskStatus.QUEUED
                            task.trace = task.trace + "\n${DateTime.now()} : Change to automatic"
                        } else if (isManual != null) {
                            return@transaction call.respond(
                                HttpStatusCode.BadRequest,
                                "The withdraw task is in ${task.status} state and can not change to ${if (isManual) "manual" else "automat"}."
                            )
                        }

                        if (finalNetworkFee != null && txid != null) {
                            if (task.status != TaskStatus.WAITING_MANUAL)
                                return@transaction call.respond(
                                    HttpStatusCode.BadRequest,
                                    "The withdraw task is in ${task.status} state and can not be submitted manually."
                                )

                            task.txid = txid
                            task.finalNetworkFee = finalNetworkFee
                            task.paidAt = DateTime.now()
                            task.status = TaskStatus.PUSHED
                            task.trace = task.trace + "\n${DateTime.now()} : Submit manual withdrawal info"
                        } else if (finalNetworkFee != null || txid != null) {
                            return@transaction call.respond(
                                HttpStatusCode.BadRequest,
                                "Please provide valid values 'finalNetworkFee' and 'txid' to submit a manual withdrawal."
                            )
                        }
                    }
                    call.respond(task!!)
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.toString())
            }
        }
    }
}
package stacrypt.stawallet.model

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.vendors.FunctionProvider

class Concat<T>(val expr: Expression<*>, vararg val items: String) : Function<T?>(VarCharColumnType()) {
    override fun toSQL(queryBuilder: QueryBuilder): String =
        TransactionManager.current().db.dialect.functionProvider.concat(expr, queryBuilder, *items)
}

fun FunctionProvider.concat(expr: Expression<*>, builder: QueryBuilder, vararg items: String) =
    "CONCAT(${expr.toSQL(builder)} , '${items.joinToString(" ,")}')"


fun <T : Any?> Column<T>.concat(vararg items: String): Concat<T> =
    Concat<T>(this, *items)

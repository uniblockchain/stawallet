package stacrypt.stawallet.model

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.vendors.FunctionProvider

//class Concat<T>(val expr: Expression<*>, vararg val items: String) : Function<T?>(VarCharColumnType()) {
//    override fun toSQL(queryBuilder: QueryBuilder): String =
//        TransactionManager.current().db.dialect.functionProvider.concat(expr, queryBuilder, *items)
//}
//
//fun FunctionProvider.concat(expr: Expression<*>, builder: QueryBuilder, vararg items: String) =
//    "CONCAT(${expr.toSQL(builder)} , '${items.joinToString(" ,")}')"
//
//fun <T : String?> Column<T>.concat(vararg items: String): Concat<T> =
//    Concat(this, *items)

class Concat<T: String?>(vararg val expr: Expression<*>) : Function<T?>(VarCharColumnType()) {
    override fun toSQL(queryBuilder: QueryBuilder): String =
        "CONCAT(${expr.joinToString(",") { it.toSQL(queryBuilder) }})"
}

class ConcatWS<T : String?>(val separator: String, vararg val expr: Expression<*>) : Function<T?>(VarCharColumnType()) {
    override fun toSQL(queryBuilder: QueryBuilder): String =
        "CONCAT_WS('$separator', ${expr.joinToString(",") { it.toSQL(queryBuilder) }})"
}

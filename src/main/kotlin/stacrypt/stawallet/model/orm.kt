package stacrypt.stawallet.model

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.vendors.FunctionProvider
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

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

class Concat<T : String?>(vararg val expr: Expression<*>) : Function<T?>(VarCharColumnType()) {
    override fun toSQL(queryBuilder: QueryBuilder): String =
        "CONCAT(${expr.joinToString(",") { it.toSQL(queryBuilder) }})"
}

class ConcatWS<T : String?>(val separator: String, vararg val expr: Expression<*>) : Function<T?>(VarCharColumnType()) {
    override fun toSQL(queryBuilder: QueryBuilder): String =
        "CONCAT_WS('$separator', ${expr.joinToString(",") { it.toSQL(queryBuilder) }})"
}

fun Table.biginteger(name: String, precision: Int): Column<BigInteger> =
    registerColumn(name, BigIntegerColumnType(precision))

class BigIntegerColumnType(val precision: Int) : ColumnType() {
    override fun sqlType(): String = "DECIMAL($precision, 0)"
    override fun valueFromDB(value: Any): Any {
        val valueFromDB = super.valueFromDB(value)
        return when (valueFromDB) {
            is BigInteger -> valueFromDB
            is BigDecimal -> valueFromDB.setScale(0, RoundingMode.UNNECESSARY).toBigInteger()
            is Double -> BigDecimal.valueOf(valueFromDB).setScale(0, RoundingMode.UNNECESSARY).toBigInteger()
            is Float -> BigDecimal(java.lang.Float.toString(valueFromDB)).setScale(0, RoundingMode.UNNECESSARY).toBigInteger()
            is Int -> valueFromDB.toBigInteger()
            is Long -> BigDecimal.valueOf(valueFromDB)
            else -> valueFromDB
        }
    }
}

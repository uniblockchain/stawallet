package stacrypt.stawallet

import java.math.BigInteger
import kotlin.math.roundToLong

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Iterable<T>.sumByBigInteger(selector: (T) -> BigInteger): BigInteger {
    var sum = 0.toBigInteger()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun max(x: BigInteger, y: BigInteger): BigInteger = if (x >= y) x else y



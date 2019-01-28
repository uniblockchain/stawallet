package stacrypt.stawallet.bitcoin

import kotlin.math.roundToLong

fun Double.btcToSat() = (this * 100_000_000.0).roundToLong()
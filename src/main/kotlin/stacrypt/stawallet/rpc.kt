//package stacrypt.stawallet
//
//class RpcConnectionException : Exception("")
//
//abstract class RpcClient(
//    userName: String,
//    password: String,
//    val host: String,
//    val posrt: Int,
//    val tls: Boolean = false
//) {
//    companion object Factory
//
//    /**
//     * This method ensure the client is still connected.
//     */
//    abstract fun ping(): Any
//}
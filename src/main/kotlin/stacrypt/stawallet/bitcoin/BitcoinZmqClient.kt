package stacrypt.stawallet.bitcoin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.walleth.khex.toHexString
import org.walleth.khex.toNoPrefixHexString
import org.zeromq.SocketType
import org.zeromq.ZMQ
import org.zeromq.ZMsg
import stacrypt.stawallet.config
import java.lang.Exception
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList

private val logger = Logger.getLogger("wallet")

data class BitcoindZmqObservable(val message: BitcoindZmqMessage) : Observable()
data class BitcoindZmqMessage(val messageType: String, val message: String)


const val ZMQ_MESSAGE_TYPE_RAW_BLOCK = "rawblock"
const val ZMQ_MESSAGE_TYPE_RAW_TX = "rawtx"
const val ZMQ_MESSAGE_TYPE_HASH_TX = "rawblock"
const val ZMQ_MESSAGE_TYPE_HASH_BLOCK = "hashblock"

val allZmqMessageTypes =
    listOf(ZMQ_MESSAGE_TYPE_RAW_BLOCK, ZMQ_MESSAGE_TYPE_RAW_TX, ZMQ_MESSAGE_TYPE_HASH_TX, ZMQ_MESSAGE_TYPE_HASH_BLOCK)

/**
 *
 *     ZMQ client
 *
 *
 *     Bitcoin should be started with the command line arguments:
 *
 *     bitcoind -testnet -daemon \
 *          -zmqpubrawtx=tcp://127.0.0.1:28332 \
 *          -zmqpubrawblock=tcp://127.0.0.1:28332 \
 *          -zmqpubhashtx=tcp://127.0.0.1:28332 \
 *          -zmqpubhashblock=tcp://127.0.0.1:28332
 *
 *
 */
object BitcoinZmqClient {

    private var isActive = false
    private val observers: ArrayList<(BitcoindZmqMessage) -> Unit> = arrayListOf()

    private fun ensureActive() {
        GlobalScope.launch(Dispatchers.IO) {
            isActive = true
            startBlocking(config.getString("daemons.bitcoind.zmq.host"), config.getInt("daemons.bitcoind.zmq.port"))
            isActive = false
            // TODO: Restart
        }
    }

    fun startBlocking(host: String, port: Int) {
        val context = ZMQ.context(1)

        // Connect our subscriber socket
        val subscriber = context.socket(SocketType.SUB)
        // Synchronize with the publisher
        subscriber.connect("tcp://$host:$port")

//        subscriber.subscribe(ZMQ_MESSAGE_TYPE_RAW_BLOCK.toByteArray())
//        subscriber.subscribe(ZMQ_MESSAGE_TYPE_RAW_TX.toByteArray())
        subscriber.subscribe(ZMQ_MESSAGE_TYPE_HASH_TX.toByteArray())
//        subscriber.subscribe(ZMQ_MESSAGE_TYPE_HASH_BLOCK.toByteArray())

        println("Subscribed.. Waiting for messages.")
        while (true) {

            try {

                val zMsg = ZMsg.recvMsg(subscriber)
                println("New message received!")
                var messageType: String? = null
                var message: String? = null
                zMsg.forEachIndexed { messageNumber, f ->
                    val bytes = f.data
                    println("Message number: " + messageNumber + " | Byte array length: " + bytes.size)
                    if (messageNumber == 0) {
                        messageType = String(bytes)
                        println("Message type: $messageType")
                    } else if (messageNumber == 1) {
                        message = bytes.toNoPrefixHexString()
                        println("Message: $message")
                    }
                }
                if (messageType != null && message != null && allZmqMessageTypes.contains(messageType!!)) {
                    val newMessage = BitcoindZmqMessage(messageType!!, message!!)
                    observers.forEach {
                        it.invoke(newMessage)
                    }
                } else {
                    logger.info("Invalid message!")
                }

            } catch (e: Exception) {
                logger.info("Error listening!")
                e.printStackTrace()
            }

        }

    }

    fun addObserver(o: (BitcoindZmqMessage) -> Unit) {
        ensureActive()
        observers.add(o)
    }

    fun removeObserver(o: (BitcoindZmqMessage) -> Unit) {
        observers.add(o)
    }

}

fun main() {
    BitcoinZmqClient.startBlocking("116.203.56.76", 28332)
}
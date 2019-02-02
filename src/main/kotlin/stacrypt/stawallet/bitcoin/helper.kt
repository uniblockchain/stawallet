package stacrypt.stawallet.bitcoin

import org.kethereum.crypto.CURVE
import org.kethereum.crypto.model.ECKeyPair
import org.kethereum.crypto.model.PUBLIC_KEY_SIZE
import org.kethereum.encodings.encodeToBase58WithChecksum
import org.kethereum.extensions.toBigInteger
import org.kethereum.extensions.toByteArray
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toMinimalByteArray
import org.kethereum.hashes.sha256
import org.kethereum.ripemd160.calculateRIPEMD160
import org.walleth.khex.hexToByteArray
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import kotlin.math.roundToLong
import java.security.spec.X509EncodedKeySpec


fun Double.btcToSat() = (this * 100_000_000.0).roundToLong()

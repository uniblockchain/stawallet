package stacrypt.stawallet.draft

import org.kethereum.bip32.model.Seed
import org.kethereum.bip32.toKey
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toCredentials
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toHexString

fun main(args: Array<String>) {
//    val m = MnemonicWords("liar orient siege thumb try certain next fit weird simple divorce circle")
//    val s =
//        Seed("ce2e130ce9d42afd52fd69c218a7cb8412b2aaabc6d98d2e28883cfbca38ca777f6ddb37ec8b1df71a4b3b2a1999b5a74811c187f02ae650709ea0dd0484a806".toByteArray())
//    val s = Seed()

    val s = Seed("0xce2e130ce9d42afd52fd69c218a7cb8412b2aaabc6d98d2e28883cfbca38ca777f6ddb37ec8b1df71a4b3b2a1999b5a74811c187f02ae650709ea0dd0484a806".hexToByteArray())
    println(s.toKey("m/44'/60'/0'/0/0").serialize(true))

    val rawTxHex = """
        483045022100b31557e47191936cb14e013fb421b1860b5e4fd5d2bc5ec1938f4ffb1651dc8902202661c2920771fd29dd91cd4100cefb971269836da4914d970d333861819265ba014104c54f8ea9507f31a05ae325616e3024bd9878cb0a5dff780444002d731577be4e2e69c663ff2da922902a4454841aa1754c1b6292ad7d317150308d8cce0ad7ab
    """.trimIndent()

}
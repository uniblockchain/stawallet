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
}
package stacrypt.stawallet.ripple

import com.fasterxml.jackson.annotation.JsonProperty

data class GetAccountParams(
    val account: String,
    @JsonProperty("signer_lists") val signerLists: Boolean = false,
    @JsonProperty("ledger_index") val ledgerIndex: String = "current",
    val strict: String = "true",
    val command: String = "account_info"
) : Param()

data class Account(
    @JsonProperty("Account") val account: String,
    @JsonProperty("Balance") val balance: String,
    @JsonProperty("Flags") val flags: Number,
    @JsonProperty("LedgerEntryType") val ledgerEntryType: String,
    @JsonProperty("OwnerCount") val ownerCount: Number,
    @JsonProperty("PreviousTxnID") val previousTxnID: String,
    @JsonProperty("PreviousTxnLgrSeq") val previousTxnLgrSeq: Number,
    @JsonProperty("Sequence") val sequence: Number,
    val index: String,
    @JsonProperty("AccountTxnID") val accountTxnID: String?,
    @JsonProperty("Domain") val domain: String?,
    @JsonProperty("EmailHash") val emailHash: String?,
    @JsonProperty("MessageKey") val messageKey: String?,
    @JsonProperty("RegularKey") val regularKey: String?,
    @JsonProperty("TickSize") val tickSize: Int?,
    @JsonProperty("TransferRate") val transferRate: Int?
)

data class SignerEntry(
    @JsonProperty("Account") val account: String,
    @JsonProperty("SignerWeight") val signerWeight: Int
)

data class SignerList(
    @JsonProperty("LedgerEntryType") val ledgerEntryType: String,
    @JsonProperty("Flags") val flags: Int,
    @JsonProperty("PreviousTxnID") val previousTxnID: String,
    @JsonProperty("PreviousTxnLgrSeq") val previousTxnLgrSeq: Int,
    @JsonProperty("OwnerNode") val ownerNode: String,
    @JsonProperty("SignerEntries") val signerEntries: List<SignerEntry>,
    @JsonProperty("SignerListID") val signerListID: Int,
    @JsonProperty("SignerQuorum") val signerQuorum: Int
)

data class GetAccountResult(
    @JsonProperty("account_data") val accountData: Account,
    @JsonProperty("signer_lists") val signerLists: List<SignerList>,
    @JsonProperty("ledger_current_index") val ledgerCurrentIndex: Number?,
    @JsonProperty("ledger_index") val ledgerIndex: Int?,
    val validated: Boolean?
)

data class GetAccountResponse(
    val result: GetAccountResult?
) : Response()

enum class KeyType {
    @JsonProperty("secp256k1")
    Secp256k1,
    @JsonProperty("ed25519")
    Ed25519,
}

data class GenerateKeypairParams(
    val passphrase: String? = null,
    val seed: String? = null,
    @JsonProperty("seed_hex") val seedHex: String? = null,
    @JsonProperty("key_type") val keyType: KeyType = KeyType.Secp256k1,
    val command: String = "wallet_propose"
) : Param()

data class GenerateKeypairResult(
    @JsonProperty("master_seed") val masterSeed: String,
    @JsonProperty("master_seed_hex") val masterSeedHex: String,
    @JsonProperty("master_key") val masterKey: String,
    @JsonProperty("account_id") val accountId: String,
    @JsonProperty("public_key") val publicKey: String,
    @JsonProperty("public_key_hex") val publicKeyHex: String,
    val warning: String?
)

data class GenerateKeypairResponse(
    val result: GenerateKeypairResult?
) : Response()


open class Param {
    val id: String = randomSecureString(512)
}

open class Response {
    lateinit var id: String
    lateinit var status: Status
}

enum class Status {
    @JsonProperty("error")
    Error,
    @JsonProperty("success")
    Success
}

enum class ErrorType {
    @JsonProperty("unknownErr")
    UnknownErr,
    @JsonProperty("unknownCmd")
    UnknownCmd,
    @JsonProperty("jsonInvalid")
    JsonInvalid,
    @JsonProperty("missingCommand")
    MissingCommand,
    @JsonProperty("tooBusy")
    TooBusy,
    @JsonProperty("noNetwork")
    NoNetwork,
    @JsonProperty("noCurrent")
    NoCurrent,
    @JsonProperty("noClosed")
    NoClosed,
    @JsonProperty("wsTextRequired")
    WsTextRequired,
    @JsonProperty("actMalformed")
    ActMalformed,
    @JsonProperty("invalidParams")
    InvalidParams,
    @JsonProperty("actNotFound")
    ActNotFound,
    @JsonProperty("lgrNotFound")
    LgrNotFound,
    @JsonProperty("txnNotFound")
    TxnNotFound,
    @JsonProperty("amendmentBlocked")
    AmendmentBlocked,
    @JsonProperty("highFee")
    HighFee,
    @JsonProperty("internalJson")
    InternalJson,
    @JsonProperty("internalSubmit")
    InternalSubmit,
    @JsonProperty("internalTransaction")
    InternalTransaction,
    @JsonProperty("invalidTransaction")
    InvalidTransaction,
    @JsonProperty("noPath")
    NoPath,
    @JsonProperty("internal")
    Internal,
    @JsonProperty("srcActMalformed")
    SrcActMalformed,
    @JsonProperty("badSeed")
    BadSeed,
    @JsonProperty("forbidden")
    Forbidden,
    @JsonProperty("couldNotConnect")
    CouldNotConnect,
    @JsonProperty("fee_mult_max")
    FeeMultMax,
}

data class ErrorResponse(
    val status: Status,
    val error: ErrorType?,
    val id: String,
    @JsonProperty("error_code") val errorCode: Int?,
    @JsonProperty("error_message") val errorMessage: String?,
    val request: Any?
) : Throwable(errorMessage)


data class GetTrustLinesParams(
    val account: String,
    val limit: Int = 10,
    val marker: Any? = null,
    @JsonProperty("ledger_index") val ledgerIndex: Any? = "current",
    @JsonProperty("ledger_hash") val ledgerHash: String? = null,
    val peer: String? = null,
    val command: String = "account_lines"
) : Param()

data class TrustLine(
    val account: String,
    val balance: String,
    val currency: String,
    val limit: String,
    @JsonProperty("limit_peer") val limitPeer: String,
    @JsonProperty("quality_in") val qualityIn: Int,
    @JsonProperty("quality_out") val qualityOut: Int,
    @JsonProperty("no_ripple") val noRipple: Boolean?,
    @JsonProperty("no_ripple_peer") val noRipplePeer: Boolean?,
    val authorized: Boolean?,
    @JsonProperty("peer_authorized") val peerAuthorized: Boolean?,
    val freeze: Boolean?,
    @JsonProperty("freeze_peer") val freezePeer: Boolean?
)

data class GetTrustLinesResult(
    val account: String,
    val lines: List<TrustLine>,
    @JsonProperty("ledger_current_index") val ledgerCurrentIndex: Int?,
    @JsonProperty("ledger_index") val ledgerIndex: Int?,
    @JsonProperty("ledger_hash") val ledgerHash: String?,
    val marker: Any
)

data class GetTrustLinesResponse(
    val result: GetTrustLinesResult?
) : Response()


enum class TransactionType {
    Payment,
    OfferCreate,
    OfferCancel,
    TrustSet,
    AccountSet,
    SetRegularKey,
    SignerListSet,
    EscrowCreate,
    EscrowFinish,
    EscrowCancel,
    PaymentChannelCreate,
    PaymentChannelFund,
    PaymentChannelClaim
}

data class GetTxInfoParams(
    val transaction: String,
    val command: String = "tx"
) : Param()

data class Amount(
    val currency: String,
    val value: String,
    val issuer: String
)

data class Transaction(
    val hash: String? = null,
    @JsonProperty("Account") val account: String,
    @JsonProperty("TransactionType") val transactionType: TransactionType,
    @JsonProperty("Destination") val destination: String? = null,
    @JsonProperty("DestinationTag") val destinationTag: Int? = null,
    @JsonProperty("Amount") val amount: Any? = null,
    @JsonProperty("Fee") val fee: String? = null,
    @JsonProperty("Sequence") val sequence: Int? = null,
    @JsonProperty("AccountTxnID") val accountTxnID: String? = null,
    @JsonProperty("Flags") val flags: Number? = null,
    @JsonProperty("Memos") val memos: List<Map<String, String>>? = null,
    @JsonProperty("SourceTag") val sourceTag: Int? = null,
    @JsonProperty("SigningPubKey") val signingPubKey: String? = null,
    @JsonProperty("TxnSignature") val txnSignature: String? = null,
    @JsonProperty("ledger_index") val ledgerIndex: Int? = null,
    val validated: Boolean? = null
)

data class GetTxInfoResponse(
    val result: Transaction?
) : Response()

data class GetTxHistoryResult(
    val index: Int?,
    val txs: List<Transaction>?
) : Response()

data class GetTxHistoryResponse(
    val result: GetTxHistoryResult?
)

data class SubmitTxParams(
    @JsonProperty("tx_blob") val txBlob: String,
    val command: String = "submit"
) : Param()

enum class EngineResult {
    @JsonProperty("tesSUCCESS")
    TesSUCCESS
}

data class SubmitTxResult(
    @JsonProperty("engine_result") val engineResult: EngineResult,
    @JsonProperty("engine_result_code") val engineResultCode: Int,
    @JsonProperty("engine_result_message") val engineResultMessage: String,
    @JsonProperty("tx_blob") val txBlob: String?,
    @JsonProperty("tx_json") val txJson: Transaction?
)

data class SubmitTxResponse(
    val result: SubmitTxResult?
) : Response()

data class SubmitMultiSignedTxParams(
    @JsonProperty("ts_json") val txJson: Transaction,
    @JsonProperty("fail_hard") val failHard: Boolean? = null,
    val command: String = "submit_multisigned"
) : Param()

data class SubmitMultiSignedTxResponse(
    val result: SubmitTxResult?
) : Response()

data class SignTxParams(
    @JsonProperty("tx_json") val txJson: Transaction,
    val secret: String? = null,
    val seed: String? = null,
    @JsonProperty("seed_hex") val seedHex: String? = null,
    val passphrase: String? = null,
    @JsonProperty("key_type") val keyType: KeyType? = null,
    val offline: Boolean = false,
    @JsonProperty("fee_mult_max") val feeMultMax: Int? = null,
    @JsonProperty("fee_div_max") val fee_div_max: Int? = null,
    val command: String = "sign"
) : Param()

data class SignTxResult(
    @JsonProperty("ts_json") val txJson: Transaction,
    @JsonProperty("tx_blob") val txBlob: String?
) : Response()

data class SignTxResponse(
    val result: SignTxResult?
) : Response()

data class SignTxForParams(
    val account: String,
    @JsonProperty("tx_json") val txJson: Transaction,
    val secret: String? = null,
    val seed: String? = null,
    @JsonProperty("seed_hex") val seedHex: String? = null,
    val passphrase: String? = null,
    @JsonProperty("key_type") val keyType: KeyType? = null,
    val command: String = "sign_for"
) : Param()

data class SignTxForResponse(
    val result: SignTxResult?
) : Response()

data class FeeResponse(
    @JsonProperty("result") val result: FeeResult?
) : Response()

data class FeeResult(
    @JsonProperty("current_ledger_size") val currentLedgerSize: String? = null,
    @JsonProperty("current_queue_size") val currentQueueSize: String? = null,
    @JsonProperty("drops") val drops: FeeDrop? = null,
    @JsonProperty("expected_ledger_size") val expectedLedgerSize: String? = null,
    @JsonProperty("ledger_current_index") val ledgerCurrentIndex: Int? = null,
    @JsonProperty("levels") val levels: FeeLevel? = null,
    @JsonProperty("max_queue_size") val maxQueueSize: String? = null
)

data class FeeDrop(
    @JsonProperty("base_fee") val baseFee: String? = null,
    @JsonProperty("median_fee") val medianFee: String? = null,
    @JsonProperty("minimum_fee") val minimumFee: String? = null,
    @JsonProperty("open_ledger_fee") val openLedgerFee: String? = null
)

data class FeeLevel(
    @JsonProperty("median_level") val medianLevel: String? = null,
    @JsonProperty("minimum_level") val minimumLevel: String? = null,
    @JsonProperty("open_ledger_level") val openLedgerLevel: String? = null,
    @JsonProperty("reference_level") val referenceLevel: String? = null
)


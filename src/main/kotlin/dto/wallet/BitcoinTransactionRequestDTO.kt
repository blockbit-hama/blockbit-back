package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class BitcoinTransactionRequestDTO(
    var fromAddress: String = "",
    var toAddress: String = "",
    var amountSatoshi: Long = 0,
    var redeemScriptHex: String = "",
    var privateKeyHex: String = ""
)
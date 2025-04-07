package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class PartialSignatureDTO(
    var walletId: String = "",
    var transactionHash: String = "",
    var partialSignature: String = "",
    var participantIndex: Int = 0,
    var rawTransaction: String = ""
)
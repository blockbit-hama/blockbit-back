package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class EthereumTransactionRequestDTO(
    var walletId: String = "",
    var participantIndex: Int = 0,
    var toAddress: String = "",
    var amount: String = "0"
)
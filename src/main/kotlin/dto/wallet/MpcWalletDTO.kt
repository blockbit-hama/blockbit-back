package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MpcWalletDTO(
    var walletId: String = "",
    var address: String = "",
    var publicKey: String = ""
)
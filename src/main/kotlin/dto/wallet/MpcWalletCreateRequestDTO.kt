package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MpcWalletCreateRequestDTO(
    val walletName: String? = null,
    val astId: Int? = null
)
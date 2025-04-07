package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MultisigWalletDTO(
    var address: String = "",
    var redeemScript: String = "",
    var publicKeys: List<String> = emptyList(),
    var privateKeys: List<String> = emptyList()
)
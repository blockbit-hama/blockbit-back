package com.sg.dto.wallet

import kotlinx.serialization.Serializable

/**
 * 단일 UTXO 정보를 담는 DTO
 */
@Serializable
data class UTXOItemDTO(
    val txId: String = "",
    val outputIndex: Int = 0,
    val value: Long = 0L,
    val valueBTC: String = "0",
    val script: String = ""
)

/**
 * 주소의 UTXO 정보를 담는 응답 DTO
 */
@Serializable
data class UTXOResponseDTO(
    val address: String = "",
    val utxoCount: Int = 0,
    val totalBalance: Long = 0L,
    val totalBalanceBTC: String = "0",
    val utxos: List<UTXOItemDTO> = emptyList()
)

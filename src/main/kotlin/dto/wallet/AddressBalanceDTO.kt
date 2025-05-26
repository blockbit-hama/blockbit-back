package com.sg.dto.wallet

import kotlinx.serialization.Serializable

/**
 * 주소 잔액 조회 응답 DTO
 */
@Serializable
data class AddressBalanceResponseDTO(
    val address: String,
    val network: String,          // "bitcoin", "ethereum" 등
    val balance: String,          // 원시 단위 (satoshi, wei)
    val formattedBalance: String, // 읽기 쉬운 형태 (BTC, ETH)
    val unit: String,            // "BTC", "ETH" 등
    val decimals: Int = 8        // 소수점 자릿수
)

/**
 * 다중 네트워크 잔액 조회 응답 DTO
 */
@Serializable
data class MultiNetworkBalanceResponseDTO(
    val address: String,
    val balances: List<AddressBalanceResponseDTO>
) 
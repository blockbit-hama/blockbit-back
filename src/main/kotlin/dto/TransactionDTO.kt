package com.sg.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.LocalDateTime

@Serializable
data class TransactionDTO(
    val trxNum: Int? = null,
    val trxHash: String? = null,
    val trxType: String? = null,
    val trxAmount: Double? = null,
    val trxFee: Double? = null,
    val trxStatus: String? = null,
    @Contextual
    val trxConfirmedAt: LocalDateTime? = null,
    val trxMemo: String? = null,
    val walId: Int? = null,
    val astId: Int? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)

@Serializable
data class TransactionResponseDTO(
    val trxNum: Int,
    val trxHash: String,
    val trxType: String,
    val trxAmount: Double,
    val trxFee: Double?,
    val trxStatus: String,
    @Contextual
    val trxConfirmedAt: LocalDateTime?,
    val trxMemo: String?,
    val walId: Int,
    val astId: Int,
    val walletName: String? = null,
    val assetSymbol: String? = null,
    val approvalsCount: Int? = null,
    val approvalsRequired: Int? = null,
    val active: String
)

@Serializable
data class TransactionDetailDTO(
    val transaction: TransactionResponseDTO,
    val approvals: List<ApprovalResponseDTO> = emptyList()
)

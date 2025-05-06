package com.sg.dto

import kotlinx.serialization.Serializable

@Serializable
data class BalanceDTO(
    val balNum: Int? = null,
    val adrId: Int? = null,
    val astId: Int? = null,
    val balBefore: Double? = null,
    val balAfter: Double? = null,
    val balConfirmed: Double? = null,
    val balPending: Double? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)

@Serializable
data class BalanceResponseDTO(
    val balNum: Int,
    val adrId: Int,
    val astId: Int,
    val balBefore: Double,
    val balAfter: Double,
    val balConfirmed: Double,
    val balPending: Double,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String
)

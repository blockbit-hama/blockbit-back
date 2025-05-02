package com.sg.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletDTO(
    val walNum: Int? = null,
    val walName: String,
    val walType: String,
    val walProtocol: String,
    val walPwd: String? = null,
    val walStatus: String,
    val usiNum: Int? = null,
    val astId: Int? = null,
    val polId: Int? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)

@Serializable
data class WalletResponseDTO(
    val walNum: Int,
    val walName: String,
    val walType: String,
    val walProtocol: String,
    val walStatus: String,
    val usiNum: Int? = null,
    val astId: Int? = null,
    val polId: Int? = null,
    val active: String
)

@Serializable
data class WalletUpdateDTO(
    val walNum: Int,
    val walName: String? = null,
    val walType: String? = null,
    val walProtocol: String? = null,
    val walPwd: String? = null,
    val walStatus: String? = null,
    val usiNum: Int? = null,
    val astId: Int? = null,
    val polId: Int? = null,
    val active: String? = null
)

@Serializable
data class WalletCreateResponseDTO(
    val walNum: Int,
    val message: String
)

@Serializable
data class WalletStatusUpdateDTO(
    val walNum: Int,
    val walStatus: String
)

@Serializable
data class MessageResponseDTO(
    val message: String
)

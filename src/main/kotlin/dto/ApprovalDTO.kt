package com.sg.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApprovalDTO(
    val aprNum: Int? = null,
    val trxId: Int? = null,
    val usiNum: Int? = null,
    val aprStatus: String? = null,
    val aprComment: String? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)

@Serializable
data class ApprovalResponseDTO(
    val aprNum: Int,
    val trxId: Int,
    val usiNum: Int,
    val aprStatus: String,
    val aprComment: String?,
    val userName: String? = null,
    val active: String
)

@Serializable
data class ApprovalRequestDTO(
    val trxId: Int,
    val usiNum: Int,
    val aprStatus: String,
    val aprComment: String? = null
)

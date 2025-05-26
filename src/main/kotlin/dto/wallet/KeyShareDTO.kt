package com.sg.dto.wallet

import kotlinx.serialization.Serializable

@Serializable
data class KeyShareDTO(
    val kshNum: Int? = null,
    val walletId: String,
    val participantIndex: Int,
    val shareX: Int,
    val shareY: String,  // 암호화된 Y 값
    val publicKey: String? = null,
    val walNum: Int? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)
package com.sg.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddressDTO(
    val adrNum: Int? = null,
    val adrAddress: String,
    val adrLabel: String? = null,
    val adrType: String,
    val adrPath: String? = null,
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
data class AddressResponseDTO(
    val adrNum: Int,
    val adrAddress: String,
    val adrLabel: String? = null,
    val adrType: String,
    val adrPath: String? = null,
    val walId: Int? = null,
    val astId: Int? = null,
    val active: String
)

@Serializable
data class AddressUpdateDTO(
    val adrNum: Int,
    val adrLabel: String? = null,
    val adrType: String? = null,
    val active: String? = null
)

@Serializable
data class AddressCreateResponseDTO(
    val adrNum: Int,
    val message: String
)

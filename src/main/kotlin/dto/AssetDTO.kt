package com.sg.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssetDTO(
    val astNum: Int? = null,
    val astName: String,
    val astSymbol: String,
    val astType: String,
    val astNetwork: String,
    val astDecimals: Int? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)

@Serializable
data class AssetResponseDTO(
    val astNum: Int,
    val astName: String,
    val astSymbol: String,
    val astType: String,
    val astNetwork: String,
    val astDecimals: Int? = null,
    val active: String
)

@Serializable
data class AssetUpdateDTO(
    val astNum: Int,
    val astName: String? = null,
    val astSymbol: String? = null,
    val astType: String? = null,
    val astNetwork: String? = null,
    val astDecimals: Int? = null,
    val active: String? = null
)

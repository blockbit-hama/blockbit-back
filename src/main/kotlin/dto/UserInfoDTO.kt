package com.sg.dto

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class UserInfoDTO(
    val usiNum: Int? = null,
    val usiId: String,
    val usiPwd: String? = null,
    val usiName: String,
    val usiPhoneNum: String,
    val usiEmail: String,
    val usiLoginDat: String? = null,
    val usiLoginTim: String? = null,
    val usiLastLoginDat: String? = null,
    val usiLastLoginTim: String? = null,
    val creusr: Int? = null,
    val credat: String? = null,
    val cretim: String? = null,
    val lmousr: Int? = null,
    val lmodat: String? = null,
    val lmotim: String? = null,
    val active: String = "1"
)

// Password만 변경하는 DTO
@Serializable
data class ChangePasswordDTO(
    val usiEmail: String,
    val currentPassword: String,
    val newPassword: String
)

// 사용자 로그인 DTO
@Serializable
data class LoginDTO(
    val usiEmail: String,
    val usiPwd: String
)

// 로그인 응답 DTO
@Serializable
data class LoginResponseDTO(
    val usiNum: Int,
    val usiEmail: String,
    val usiName: String,
    val success: Boolean = true,
    val message: String = "로그인 성공",
    val token: String? = null
)

// 비밀번호를 제외한 사용자 정보를 반환하는 DTO
@Serializable
data class UserInfoResponseDTO(
    val usiNum: Int,
    val usiId: String,
    val usiName: String,
    val usiPhoneNum: String,
    val usiEmail: String,
    val usiLoginDat: String? = null,
    val usiLoginTim: String? = null,
    val usiLastLoginDat: String? = null,
    val usiLastLoginTim: String? = null,
    val active: String
)

// 현재 날짜와 시간을 얻는 유틸리티 함수
object DateTimeUtil {
    fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    fun getCurrentTime(): String {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
    }
}

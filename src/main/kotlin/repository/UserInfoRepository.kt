package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.UserInfoDTO
import com.sg.dto.UserInfoResponseDTO
import org.jetbrains.exposed.sql.*
import java.security.MessageDigest

object UserInfoTable : Table("user_info") {
    val usiNum = integer("usi_num").autoIncrement()
    val usiId = varchar("usi_id", 50)
    val usiPwd = varchar("usi_pwd", 100)
    val usiName = varchar("usi_name", 100)
    val usiPhoneNum = varchar("usi_phone_num", 20)
    val usiEmail = varchar("usi_email", 100)
    val usiLoginDat = varchar("usi_login_dat", 8).nullable()
    val usiLoginTim = varchar("usi_login_tim", 6).nullable()
    val usiLastLoginDat = varchar("usi_last_login_dat", 8).nullable()
    val usiLastLoginTim = varchar("usi_last_login_tim", 6).nullable()
    val creusr = integer("creusr").nullable()
    val credat = varchar("credat", 8).nullable()
    val cretim = varchar("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = varchar("lmodat", 8).nullable()
    val lmotim = varchar("lmotim", 6).nullable()
    val active = varchar("active", 1)

    override val primaryKey = PrimaryKey(usiNum)
}

class UserInfoRepository {
    // 모든 유저 조회
    suspend fun getAllUsers(): List<UserInfoResponseDTO> = dbQuery {
        UserInfoTable.selectAll()
            .map { resultRowToUserResponseDTO(it) }
    }

    // 유저 ID로 유저 정보 조회
    suspend fun getUserById(id: String): UserInfoResponseDTO? = dbQuery {
        UserInfoTable
            .select { UserInfoTable.usiId eq id }
            .map { resultRowToUserResponseDTO(it) }
            .singleOrNull()
    }

    // 유저 번호로 유저 정보 조회
    suspend fun getUserByNum(num: Int): UserInfoResponseDTO? = dbQuery {
        UserInfoTable
            .select { UserInfoTable.usiNum eq num }
            .map { resultRowToUserResponseDTO(it) }
            .singleOrNull()
    }

    // 비밀번호 체크를 위한 유저 조회 (비밀번호 포함)
    suspend fun getUserForAuthentication(email: String): UserInfoDTO? = dbQuery {
        UserInfoTable
            .select { UserInfoTable.usiEmail eq email }
            .map { resultRowToUserDTO(it) }
            .singleOrNull()
    }

    // 유저 생성
    suspend fun addUser(userInfo: UserInfoDTO): Int = dbQuery {
        // 비밀번호 해시
        val hashedPassword = hashPassword(userInfo.usiPwd ?: "")
        
        UserInfoTable.insert {
            it[usiId] = userInfo.usiId
            it[usiPwd] = hashedPassword
            it[usiName] = userInfo.usiName
            it[usiPhoneNum] = userInfo.usiPhoneNum
            it[usiEmail] = userInfo.usiEmail
            it[usiLoginDat] = userInfo.usiLoginDat
            it[usiLoginTim] = userInfo.usiLoginTim
            it[usiLastLoginDat] = userInfo.usiLastLoginDat
            it[usiLastLoginTim] = userInfo.usiLastLoginTim
            it[creusr] = userInfo.creusr
            it[credat] = userInfo.credat
            it[cretim] = userInfo.cretim
            it[lmousr] = userInfo.lmousr
            it[lmodat] = userInfo.lmodat
            it[lmotim] = userInfo.lmotim
            it[active] = userInfo.active
        }[UserInfoTable.usiNum]
    }

    // 유저 정보 업데이트
    suspend fun updateUser(userInfo: UserInfoDTO): Boolean = dbQuery {
        if (userInfo.usiNum == null) return@dbQuery false
        
        val updateResult = UserInfoTable.update({ UserInfoTable.usiNum eq userInfo.usiNum }) {
            userInfo.usiName?.let { name -> it[usiName] = name }
            userInfo.usiPhoneNum?.let { phone -> it[usiPhoneNum] = phone }
            userInfo.usiEmail?.let { email -> it[usiEmail] = email }
            userInfo.usiLoginDat?.let { loginDat -> it[usiLoginDat] = loginDat }
            userInfo.usiLoginTim?.let { loginTim -> it[usiLoginTim] = loginTim }
            userInfo.usiLastLoginDat?.let { lastLoginDat -> it[usiLastLoginDat] = lastLoginDat }
            userInfo.usiLastLoginTim?.let { lastLoginTim -> it[usiLastLoginTim] = lastLoginTim }
            userInfo.lmousr?.let { modUsr -> it[lmousr] = modUsr }
            userInfo.lmodat?.let { modDat -> it[lmodat] = modDat }
            userInfo.lmotim?.let { modTim -> it[lmotim] = modTim }
            it[active] = userInfo.active
        }
        updateResult > 0
    }

    // 비밀번호 업데이트
    suspend fun updatePassword(usiNum: Int, newPassword: String): Boolean = dbQuery {
        val hashedPassword = hashPassword(newPassword)
        val updateResult = UserInfoTable.update({ UserInfoTable.usiNum eq usiNum }) {
            it[usiPwd] = hashedPassword
        }
        updateResult > 0
    }

    // 유저 삭제 (실제 삭제가 아닌 active 상태 변경)
    suspend fun deleteUser(usiNum: Int): Boolean = dbQuery {
        val updateResult = UserInfoTable.update({ UserInfoTable.usiNum eq usiNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }

    // 유저 로그인 시간 업데이트
    suspend fun updateLoginTime(usiNum: Int, loginDat: String, loginTim: String): Boolean = dbQuery {
        val lastLoginDat = UserInfoTable.select { UserInfoTable.usiNum eq usiNum }
            .map { it[UserInfoTable.usiLoginDat] }.singleOrNull()
        val lastLoginTim = UserInfoTable.select { UserInfoTable.usiNum eq usiNum }
            .map { it[UserInfoTable.usiLoginTim] }.singleOrNull()
        
        val updateResult = UserInfoTable.update({ UserInfoTable.usiNum eq usiNum }) {
            it[usiLoginDat] = loginDat
            it[usiLoginTim] = loginTim
            if (lastLoginDat != null) it[usiLastLoginDat] = lastLoginDat
            if (lastLoginTim != null) it[usiLastLoginTim] = lastLoginTim
        }
        updateResult > 0
    }

    // 비밀번호 해시 함수
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ResultRow를 DTO로 변환하는 함수
    private fun resultRowToUserDTO(row: ResultRow) = UserInfoDTO(
        usiNum = row[UserInfoTable.usiNum],
        usiId = row[UserInfoTable.usiId],
        usiPwd = row[UserInfoTable.usiPwd],
        usiName = row[UserInfoTable.usiName],
        usiPhoneNum = row[UserInfoTable.usiPhoneNum],
        usiEmail = row[UserInfoTable.usiEmail],
        usiLoginDat = row[UserInfoTable.usiLoginDat],
        usiLoginTim = row[UserInfoTable.usiLoginTim],
        usiLastLoginDat = row[UserInfoTable.usiLastLoginDat],
        usiLastLoginTim = row[UserInfoTable.usiLastLoginTim],
        creusr = row[UserInfoTable.creusr],
        credat = row[UserInfoTable.credat],
        cretim = row[UserInfoTable.cretim],
        lmousr = row[UserInfoTable.lmousr],
        lmodat = row[UserInfoTable.lmodat],
        lmotim = row[UserInfoTable.lmotim],
        active = row[UserInfoTable.active]
    )

    // ResultRow를 응답 DTO로 변환하는 함수 (비밀번호 제외)
    private fun resultRowToUserResponseDTO(row: ResultRow) = UserInfoResponseDTO(
        usiNum = row[UserInfoTable.usiNum],
        usiId = row[UserInfoTable.usiId],
        usiName = row[UserInfoTable.usiName],
        usiPhoneNum = row[UserInfoTable.usiPhoneNum],
        usiEmail = row[UserInfoTable.usiEmail],
        usiLoginDat = row[UserInfoTable.usiLoginDat],
        usiLoginTim = row[UserInfoTable.usiLoginTim],
        usiLastLoginDat = row[UserInfoTable.usiLastLoginDat],
        usiLastLoginTim = row[UserInfoTable.usiLastLoginTim],
        active = row[UserInfoTable.active]
    )
}

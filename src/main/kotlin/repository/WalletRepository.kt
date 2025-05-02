package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.WalletDTO
import com.sg.dto.WalletResponseDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.security.MessageDigest

object WalletTable : Table("wallets") {
    val walNum = integer("wal_num").autoIncrement()
    val walName = varchar("wal_name", 100)
    val walType = varchar("wal_type", 50)
    val walProtocol = varchar("wal_protocol", 20)
    val walPwd = varchar("wal_pwd", 255).nullable()
    val walStatus = varchar("wal_status", 20)
    val usiNum = integer("usi_num").references(UserInfoTable.usiNum).nullable()
    val astId = integer("ast_id").references(AssetTable.astNum).nullable()
    val polId = integer("pol_id").nullable() // references policies table
    val creusr = integer("creusr").nullable()
    val credat = varchar("credat", 8).nullable()
    val cretim = varchar("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = varchar("lmodat", 8).nullable()
    val lmotim = varchar("lmotim", 6).nullable()
    val active = varchar("active", 1)

    override val primaryKey = PrimaryKey(walNum)
}

class WalletRepository {
    
    // 모든 지갑 조회
    suspend fun getAllWallets(): List<WalletResponseDTO> = dbQuery {
        WalletTable.selectAll()
            .map { resultRowToWalletResponseDTO(it) }
    }

    // 지갑 번호로 지갑 정보 조회
    suspend fun getWalletByNum(num: Int): WalletResponseDTO? = dbQuery {
        WalletTable
            .select { WalletTable.walNum eq num }
            .map { resultRowToWalletResponseDTO(it) }
            .singleOrNull()
    }

    // 사용자 번호로 지갑 목록 조회
    suspend fun getWalletsByUser(usiNum: Int): List<WalletResponseDTO> = dbQuery {
        WalletTable
            .select { WalletTable.usiNum eq usiNum }
            .map { resultRowToWalletResponseDTO(it) }
    }
    
    // 자산 번호로 지갑 목록 조회
    suspend fun getWalletsByAsset(astId: Int): List<WalletResponseDTO> = dbQuery {
        WalletTable
            .select { WalletTable.astId eq astId }
            .map { resultRowToWalletResponseDTO(it) }
    }
    
    // 지갑 타입으로 지갑 목록 조회
    suspend fun getWalletsByType(walType: String): List<WalletResponseDTO> = dbQuery {
        WalletTable
            .select { WalletTable.walType eq walType }
            .map { resultRowToWalletResponseDTO(it) }
    }
    
    // 지갑 프로토콜로 지갑 목록 조회
    suspend fun getWalletsByProtocol(walProtocol: String): List<WalletResponseDTO> = dbQuery {
        WalletTable
            .select { WalletTable.walProtocol eq walProtocol }
            .map { resultRowToWalletResponseDTO(it) }
    }
    
    // 지갑 상태로 지갑 목록 조회
    suspend fun getWalletsByStatus(walStatus: String): List<WalletResponseDTO> = dbQuery {
        WalletTable
            .select { WalletTable.walStatus eq walStatus }
            .map { resultRowToWalletResponseDTO(it) }
    }

    // 지갑 추가
    suspend fun addWallet(wallet: WalletDTO): Int = dbQuery {
        // 비밀번호가 있는 경우 해시
        val hashedPassword = wallet.walPwd?.let { hashPassword(it) }
        
        WalletTable.insert {
            it[walName] = wallet.walName
            it[walType] = wallet.walType
            it[walProtocol] = wallet.walProtocol
            it[walPwd] = hashedPassword
            it[walStatus] = wallet.walStatus
            it[usiNum] = wallet.usiNum
            it[astId] = wallet.astId
            it[polId] = wallet.polId
            it[creusr] = wallet.creusr
            it[credat] = wallet.credat
            it[cretim] = wallet.cretim
            it[lmousr] = wallet.lmousr
            it[lmodat] = wallet.lmodat
            it[lmotim] = wallet.lmotim
            it[active] = wallet.active
        }[WalletTable.walNum]
    }

    // 지갑 정보 업데이트
    suspend fun updateWallet(wallet: WalletDTO): Boolean = dbQuery {
        if (wallet.walNum == null) return@dbQuery false
        
        // 비밀번호가 있는 경우 해시
        val hashedPassword = wallet.walPwd?.let { hashPassword(it) }
        
        val updateResult = WalletTable.update({ WalletTable.walNum eq wallet.walNum }) {
            it[walName] = wallet.walName
            it[walType] = wallet.walType
            it[walProtocol] = wallet.walProtocol
            if (hashedPassword != null) {
                it[walPwd] = hashedPassword
            }
            it[walStatus] = wallet.walStatus
            it[usiNum] = wallet.usiNum
            it[astId] = wallet.astId
            it[polId] = wallet.polId
            it[lmousr] = wallet.lmousr
            it[lmodat] = wallet.lmodat
            it[lmotim] = wallet.lmotim
            it[active] = wallet.active
        }
        updateResult > 0
    }

    // 지갑 부분 업데이트 (일부 필드만)
    suspend fun partialUpdateWallet(wallet: WalletDTO): Boolean = dbQuery {
        if (wallet.walNum == null) return@dbQuery false
        
        // 비밀번호가 있는 경우 해시
        val hashedPassword = wallet.walPwd?.let { hashPassword(it) }
        
        // 수정할 필드 수집 (업데이트할 내용이 있는지 추적)
        var hasUpdates = false
        
        // 업데이트 실행
        val updateResult = WalletTable.update({ WalletTable.walNum eq wallet.walNum }) { 
            if (wallet.walName != null) {
                it[walName] = wallet.walName
                hasUpdates = true
            }
            if (wallet.walType != null) {
                it[walType] = wallet.walType
                hasUpdates = true
            }
            if (wallet.walProtocol != null) {
                it[walProtocol] = wallet.walProtocol
                hasUpdates = true
            }
            if (hashedPassword != null) {
                it[walPwd] = hashedPassword
                hasUpdates = true
            }
            if (wallet.walStatus != null) {
                it[walStatus] = wallet.walStatus
                hasUpdates = true
            }
            if (wallet.usiNum != null) {
                it[usiNum] = wallet.usiNum
                hasUpdates = true
            }
            if (wallet.astId != null) {
                it[astId] = wallet.astId
                hasUpdates = true
            }
            if (wallet.polId != null) {
                it[polId] = wallet.polId
                hasUpdates = true
            }
            if (wallet.lmousr != null) {
                it[lmousr] = wallet.lmousr
                hasUpdates = true
            }
            if (wallet.lmodat != null) {
                it[lmodat] = wallet.lmodat
                hasUpdates = true
            }
            if (wallet.lmotim != null) {
                it[lmotim] = wallet.lmotim
                hasUpdates = true
            }
            if (wallet.active != null) {
                it[active] = wallet.active
                hasUpdates = true
            }
        }
        
        // 업데이트할 필드가 있는 경우에만 결과 확인
        if (hasUpdates) {
            updateResult > 0
        } else {
            true // 업데이트할 필드가 없으면 성공으로 간주
        }
    }
    
    // 지갑 상태 업데이트
    suspend fun updateWalletStatus(walNum: Int, walStatus: String, userNum: Int): Boolean = dbQuery {
        val updateResult = WalletTable.update({ WalletTable.walNum eq walNum }) {
            it[WalletTable.walStatus] = walStatus
            it[lmousr] = userNum
            it[lmodat] = com.sg.dto.DateTimeUtil.getCurrentDate()
            it[lmotim] = com.sg.dto.DateTimeUtil.getCurrentTime()
        }
        updateResult > 0
    }

    // 지갑 삭제 (비활성화)
    suspend fun deleteWallet(walNum: Int): Boolean = dbQuery {
        val updateResult = WalletTable.update({ WalletTable.walNum eq walNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }

    // 비밀번호 해시 함수
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ResultRow를 응답 DTO로 변환하는 함수
    private fun resultRowToWalletResponseDTO(row: ResultRow) = WalletResponseDTO(
        walNum = row[WalletTable.walNum],
        walName = row[WalletTable.walName],
        walType = row[WalletTable.walType],
        walProtocol = row[WalletTable.walProtocol],
        walStatus = row[WalletTable.walStatus],
        usiNum = row[WalletTable.usiNum],
        astId = row[WalletTable.astId],
        polId = row[WalletTable.polId],
        active = row[WalletTable.active]
    )

    // ResultRow를 DTO로 변환하는 함수
    private fun resultRowToWalletDTO(row: ResultRow) = WalletDTO(
        walNum = row[WalletTable.walNum],
        walName = row[WalletTable.walName],
        walType = row[WalletTable.walType],
        walProtocol = row[WalletTable.walProtocol],
        walPwd = row[WalletTable.walPwd],
        walStatus = row[WalletTable.walStatus],
        usiNum = row[WalletTable.usiNum],
        astId = row[WalletTable.astId],
        polId = row[WalletTable.polId],
        creusr = row[WalletTable.creusr],
        credat = row[WalletTable.credat],
        cretim = row[WalletTable.cretim],
        lmousr = row[WalletTable.lmousr],
        lmodat = row[WalletTable.lmodat],
        lmotim = row[WalletTable.lmotim],
        active = row[WalletTable.active]
    )
}

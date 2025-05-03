package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.ApprovalDTO
import com.sg.dto.ApprovalResponseDTO
import org.jetbrains.exposed.sql.*

object ApprovalTable : Table("approvals") {
    val aprNum = integer("apr_num").autoIncrement()
    val trxId = integer("trx_id").references(TransactionTable.trxNum)
    val usiNum = integer("usi_num").references(UserInfoTable.usiNum)
    val aprStatus = varchar("apr_status", 20)
    val aprComment = text("apr_comment").nullable()
    val creusr = integer("creusr").nullable()
    val credat = char("credat", 8).nullable()
    val cretim = char("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = char("lmodat", 8).nullable()
    val lmotim = char("lmotim", 6).nullable()
    val active = char("active", 1)

    override val primaryKey = PrimaryKey(aprNum)
}

class ApprovalRepository {
    
    // 모든 승인 요청 조회
    suspend fun getAllApprovals(): List<ApprovalResponseDTO> = dbQuery {
        val userJoin = ApprovalTable.join(UserInfoTable, JoinType.LEFT,
            additionalConstraint = { ApprovalTable.usiNum eq UserInfoTable.usiNum })
        
        userJoin.selectAll()
            .map { resultRowToApprovalResponseDTO(it) }
    }
    
    // 특정 트랜잭션의 승인 요청 조회
    suspend fun getApprovalsByTransactionId(trxId: Int): List<ApprovalResponseDTO> = dbQuery {
        val userJoin = ApprovalTable.join(UserInfoTable, JoinType.LEFT,
            additionalConstraint = { ApprovalTable.usiNum eq UserInfoTable.usiNum })
        
        userJoin.select { ApprovalTable.trxId eq trxId }
            .map { resultRowToApprovalResponseDTO(it) }
    }
    
    // 특정 사용자의 승인 요청 조회
    suspend fun getApprovalsByUserId(usiNum: Int): List<ApprovalResponseDTO> = dbQuery {
        val userJoin = ApprovalTable.join(UserInfoTable, JoinType.LEFT,
            additionalConstraint = { ApprovalTable.usiNum eq UserInfoTable.usiNum })
        
        userJoin.select { ApprovalTable.usiNum eq usiNum }
            .map { resultRowToApprovalResponseDTO(it) }
    }
    
    // 특정 승인 요청 조회
    suspend fun getApprovalById(aprNum: Int): ApprovalResponseDTO? = dbQuery {
        val userJoin = ApprovalTable.join(UserInfoTable, JoinType.LEFT,
            additionalConstraint = { ApprovalTable.usiNum eq UserInfoTable.usiNum })
        
        userJoin.select { ApprovalTable.aprNum eq aprNum }
            .map { resultRowToApprovalResponseDTO(it) }
            .singleOrNull()
    }
    
    // 승인 요청 추가
    suspend fun addApproval(approval: ApprovalDTO): Int = dbQuery {
        ApprovalTable.insert {
            it[trxId] = approval.trxId ?: error("Transaction ID is required")
            it[usiNum] = approval.usiNum ?: error("User ID is required")
            it[aprStatus] = approval.aprStatus ?: "pending"
            it[aprComment] = approval.aprComment
            it[creusr] = approval.creusr
            it[credat] = approval.credat
            it[cretim] = approval.cretim
            it[lmousr] = approval.lmousr
            it[lmodat] = approval.lmodat
            it[lmotim] = approval.lmotim
            it[active] = approval.active
        }[ApprovalTable.aprNum]
    }
    
    // 승인 상태 업데이트
    suspend fun updateApprovalStatus(aprNum: Int, status: String, comment: String? = null): Boolean = dbQuery {
        val updateResult = ApprovalTable.update({ ApprovalTable.aprNum eq aprNum }) {
            it[aprStatus] = status
            if (comment != null) {
                it[aprComment] = comment
            }
        }
        updateResult > 0
    }
    
    // 특정 트랜잭션의 승인 개수 조회
    suspend fun countApprovalsByTransaction(trxId: Int, status: String? = null): Int = dbQuery {
        val query = if (status != null) {
            ApprovalTable.select { (ApprovalTable.trxId eq trxId) and (ApprovalTable.aprStatus eq status) }
        } else {
            ApprovalTable.select { ApprovalTable.trxId eq trxId }
        }
        query.count().toInt()
    }
    
    // 승인 요청 삭제 (비활성화)
    suspend fun deleteApproval(aprNum: Int): Boolean = dbQuery {
        val updateResult = ApprovalTable.update({ ApprovalTable.aprNum eq aprNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }
    
    // ResultRow를 DTO로 변환하는 함수
    private fun resultRowToApprovalDTO(row: ResultRow) = ApprovalDTO(
        aprNum = row[ApprovalTable.aprNum],
        trxId = row[ApprovalTable.trxId],
        usiNum = row[ApprovalTable.usiNum],
        aprStatus = row[ApprovalTable.aprStatus],
        aprComment = row[ApprovalTable.aprComment],
        creusr = row[ApprovalTable.creusr],
        credat = row[ApprovalTable.credat],
        cretim = row[ApprovalTable.cretim],
        lmousr = row[ApprovalTable.lmousr],
        lmodat = row[ApprovalTable.lmodat],
        lmotim = row[ApprovalTable.lmotim],
        active = row[ApprovalTable.active]
    )
    
    // ResultRow를 응답 DTO로 변환하는 함수 (사용자 이름 포함)
    private fun resultRowToApprovalResponseDTO(row: ResultRow) = ApprovalResponseDTO(
        aprNum = row[ApprovalTable.aprNum],
        trxId = row[ApprovalTable.trxId],
        usiNum = row[ApprovalTable.usiNum],
        aprStatus = row[ApprovalTable.aprStatus],
        aprComment = row[ApprovalTable.aprComment],
        userName = if (row.hasValue(UserInfoTable.usiName)) row[UserInfoTable.usiName] else null,
        active = row[ApprovalTable.active]
    )
}

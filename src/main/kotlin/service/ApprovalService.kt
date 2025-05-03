package com.sg.service

import com.sg.dto.ApprovalDTO
import com.sg.dto.ApprovalRequestDTO
import com.sg.dto.ApprovalResponseDTO
import com.sg.repository.ApprovalRepository
import com.sg.repository.TransactionRepository
import com.sg.utils.DateTimeUtil

class ApprovalService(
    private val repository: ApprovalRepository = ApprovalRepository(),
    private val transactionService: TransactionService? = null
) {
    
    // 모든 승인 요청 조회
    suspend fun getAllApprovals(): List<ApprovalResponseDTO> {
        return repository.getAllApprovals()
    }
    
    // 트랜잭션별 승인 요청 조회
    suspend fun getApprovalsByTransactionId(trxId: Int): List<ApprovalResponseDTO> {
        return repository.getApprovalsByTransactionId(trxId)
    }
    
    // 사용자별 승인 요청 조회
    suspend fun getApprovalsByUserId(usiNum: Int): List<ApprovalResponseDTO> {
        return repository.getApprovalsByUserId(usiNum)
    }
    
    // 특정 승인 요청 조회
    suspend fun getApprovalById(aprNum: Int): ApprovalResponseDTO? {
        return repository.getApprovalById(aprNum)
    }
    
    // 승인 요청 추가
    suspend fun addApproval(request: ApprovalRequestDTO, creusr: Int? = null): Int {
        // 생성 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val approval = ApprovalDTO(
            trxId = request.trxId,
            usiNum = request.usiNum,
            aprStatus = request.aprStatus,
            aprComment = request.aprComment,
            creusr = creusr ?: request.usiNum,
            credat = currentDate,
            cretim = currentTime,
            lmousr = creusr ?: request.usiNum,
            lmodat = currentDate,
            lmotim = currentTime,
            active = "1"
        )
        
        val aprNum = repository.addApproval(approval)
        
        // TransactionService가 주입된 경우, 트랜잭션 상태 업데이트 확인
        if (transactionService != null && request.aprStatus == "approved") {
            transactionService.checkAndUpdateTransactionStatus(request.trxId)
        }
        
        return aprNum
    }
    
    // 승인 상태 업데이트
    suspend fun updateApprovalStatus(aprNum: Int, status: String, comment: String? = null, usiNum: Int? = null): Boolean {
        // 업데이트 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val approval = getApprovalById(aprNum) ?: return false
        
        // 상태 업데이트
        val result = repository.updateApprovalStatus(aprNum, status, comment)
        
        // 상태가 업데이트되었고 TransactionService가 주입된 경우, 트랜잭션 상태 업데이트 확인
        if (result && transactionService != null && status == "approved") {
            transactionService.checkAndUpdateTransactionStatus(approval.trxId)
        }
        
        return result
    }
    
    // 승인 요청 삭제 (비활성화)
    suspend fun deleteApproval(aprNum: Int): Boolean {
        return repository.deleteApproval(aprNum)
    }
    
    // 특정 트랜잭션의 승인 개수 조회
    suspend fun countApprovalsByTransaction(trxId: Int, status: String? = null): Int {
        return repository.countApprovalsByTransaction(trxId, status)
    }
}

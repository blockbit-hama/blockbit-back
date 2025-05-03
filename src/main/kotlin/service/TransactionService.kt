package com.sg.service

import com.sg.dto.TransactionDTO
import com.sg.dto.TransactionDetailDTO
import com.sg.dto.TransactionResponseDTO
import com.sg.repository.ApprovalRepository
import com.sg.repository.TransactionRepository
import com.sg.repository.WalletRepository
import com.sg.utils.DateTimeUtil
import java.time.LocalDateTime

class TransactionService(
    private val repository: TransactionRepository = TransactionRepository(),
    private val approvalRepository: ApprovalRepository = ApprovalRepository(),
    private val walletRepository: WalletRepository = WalletRepository()
) {
    
    // 모든 트랜잭션 조회
    suspend fun getAllTransactions(): List<TransactionResponseDTO> {
        return repository.getAllTransactions()
    }
    
    // 지갑별 트랜잭션 조회
    suspend fun getTransactionsByWalletId(walletId: Int): List<TransactionResponseDTO> {
        return repository.getTransactionsByWalletId(walletId)
    }
    
    // 특정 트랜잭션 조회 (승인 정보 포함)
    suspend fun getTransactionDetails(trxNum: Int): TransactionDetailDTO? {
        val transaction = repository.getTransactionById(trxNum) ?: return null
        val approvals = approvalRepository.getApprovalsByTransactionId(trxNum)
        
        // 지갑의 승인 임계값 정보를 조회
        val wallet = walletRepository.getWalletByNum(transaction.walId) ?: return null
        val approvalThreshold = wallet.polId?.let { 
            walletRepository.getApprovalThreshold(it)
        }
        
        // 승인 정보를 포함한 트랜잭션 상세 정보 생성
        val transactionWithApprovalInfo = transaction.copy(
            approvalsCount = approvals.size,
            approvalsRequired = approvalThreshold
        )
        
        return TransactionDetailDTO(
            transaction = transactionWithApprovalInfo,
            approvals = approvals
        )
    }
    
    // 트랜잭션 추가
    suspend fun addTransaction(transaction: TransactionDTO): Int {
        // 생성 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val transactionWithTimestamp = transaction.copy(
            credat = currentDate,
            cretim = currentTime,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.addTransaction(transactionWithTimestamp)
    }
    
    // 트랜잭션 업데이트
    suspend fun updateTransaction(transaction: TransactionDTO): Boolean {
        // 업데이트 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val transactionWithTimestamp = transaction.copy(
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.updateTransaction(transactionWithTimestamp)
    }
    
    // 트랜잭션 상태 업데이트
    suspend fun updateTransactionStatus(trxNum: Int, status: String): Boolean {
        // 확인 시간을 현재 시간으로 설정
        val confirmedAt = if (status == "confirmed") LocalDateTime.now() else null
        
        return repository.updateTransactionStatus(trxNum, status, confirmedAt)
    }
    
    // 트랜잭션 삭제 (비활성화)
    suspend fun deleteTransaction(trxNum: Int): Boolean {
        return repository.deleteTransaction(trxNum)
    }
    
    // 승인 상태에 따른 트랜잭션 상태 업데이트 확인
    suspend fun checkAndUpdateTransactionStatus(trxNum: Int): Boolean {
        val transaction = repository.getTransactionById(trxNum) ?: return false
        val wallet = walletRepository.getWalletByNum(transaction.walId) ?: return false
        
        // 지갑에 연결된 정책이 있는 경우
        if (wallet.polId != null) {
            val approvalThreshold = walletRepository.getApprovalThreshold(wallet.polId)
            if (approvalThreshold != null) {
                // 승인된 승인 요청 개수 조회
                val approvedCount = approvalRepository.countApprovalsByTransaction(trxNum, "approved")
                
                // 승인 임계값 이상의 승인이 있으면 트랜잭션 상태를 confirmed로 업데이트
                if (approvedCount >= approvalThreshold) {
                    return updateTransactionStatus(trxNum, "confirmed")
                }
            }
        }
        
        return false
    }
}

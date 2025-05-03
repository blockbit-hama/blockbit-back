package com.sg.service

import com.sg.dto.BalanceDTO
import com.sg.dto.BalanceResponseDTO
import com.sg.repository.BalanceRepository
import com.sg.utils.DateTimeUtil

class BalanceService(private val repository: BalanceRepository = BalanceRepository()) {
    
    // 모든 잔액 조회
    suspend fun getAllBalances(): List<BalanceResponseDTO> {
        return repository.getAllBalances()
    }
    
    // 주소별 잔액 조회
    suspend fun getBalancesByAddressId(addressId: Int): List<BalanceResponseDTO> {
        return repository.getBalancesByAddressId(addressId)
    }
    
    // 자산별 잔액 조회
    suspend fun getBalancesByAssetId(assetId: Int): List<BalanceResponseDTO> {
        return repository.getBalancesByAssetId(assetId)
    }
    
    // 특정 잔액 조회
    suspend fun getBalanceById(balNum: Int): BalanceResponseDTO? {
        return repository.getBalanceById(balNum)
    }
    
    // 잔액 추가
    suspend fun addBalance(balance: BalanceDTO): Int {
        // 생성 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val balanceWithTimestamp = balance.copy(
            credat = currentDate,
            cretim = currentTime,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.addBalance(balanceWithTimestamp)
    }
    
    // 잔액 업데이트
    suspend fun updateBalance(balance: BalanceDTO): Boolean {
        // 업데이트 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val balanceWithTimestamp = balance.copy(
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.updateBalance(balanceWithTimestamp)
    }
    
    // 잔액 삭제 (비활성화)
    suspend fun deleteBalance(balNum: Int): Boolean {
        return repository.deleteBalance(balNum)
    }
}

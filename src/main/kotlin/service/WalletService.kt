package com.sg.service

import com.sg.dto.WalletDTO
import com.sg.dto.WalletResponseDTO
import com.sg.dto.WalletUpdateDTO
import com.sg.dto.DateTimeUtil
import com.sg.repository.WalletRepository

class WalletService(private val repository: WalletRepository = WalletRepository()) {

    // 모든 지갑 목록 조회
    suspend fun getAllWallets(): List<WalletResponseDTO> {
        return repository.getAllWallets()
    }

    // 지갑 번호로 지갑 정보 조회
    suspend fun getWalletByNum(num: Int): WalletResponseDTO? {
        return repository.getWalletByNum(num)
    }

    // 사용자 번호로 지갑 목록 조회
    suspend fun getWalletsByUser(usiNum: Int): List<WalletResponseDTO> {
        return repository.getWalletsByUser(usiNum)
    }
    
    // 자산 번호로 지갑 목록 조회
    suspend fun getWalletsByAsset(astId: Int): List<WalletResponseDTO> {
        return repository.getWalletsByAsset(astId)
    }
    
    // 지갑 타입으로 지갑 목록 조회
    suspend fun getWalletsByType(walType: String): List<WalletResponseDTO> {
        return repository.getWalletsByType(walType)
    }
    
    // 지갑 프로토콜로 지갑 목록 조회
    suspend fun getWalletsByProtocol(walProtocol: String): List<WalletResponseDTO> {
        return repository.getWalletsByProtocol(walProtocol)
    }
    
    // 지갑 상태로 지갑 목록 조회
    suspend fun getWalletsByStatus(walStatus: String): List<WalletResponseDTO> {
        return repository.getWalletsByStatus(walStatus)
    }

    // 새 지갑 추가
    suspend fun addWallet(wallet: WalletDTO, userNum: Int): Int {
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val walletWithTimestamp = wallet.copy(
            creusr = userNum,
            credat = currentDate,
            cretim = currentTime,
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.addWallet(walletWithTimestamp)
    }

    // 지갑 정보 업데이트
    suspend fun updateWallet(wallet: WalletDTO, userNum: Int): Boolean {
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val walletWithTimestamp = wallet.copy(
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.updateWallet(walletWithTimestamp)
    }

    // 지갑 정보 부분 업데이트
    suspend fun partialUpdateWallet(updateDTO: WalletUpdateDTO, userNum: Int): Boolean {
        // 기존 지갑 정보 조회
        val existingWallet = repository.getWalletByNum(updateDTO.walNum)
            ?: return false // 지갑이 존재하지 않으면 업데이트 실패
        
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        // 업데이트할 필드만 변경하여 DTO 생성
        val walletToUpdate = WalletDTO(
            walNum = updateDTO.walNum,
            walName = updateDTO.walName ?: existingWallet.walName,
            walType = updateDTO.walType ?: existingWallet.walType,
            walProtocol = updateDTO.walProtocol ?: existingWallet.walProtocol,
            walPwd = updateDTO.walPwd, // 비밀번호는 해시 처리가 필요하므로 그대로 전달
            walStatus = updateDTO.walStatus ?: existingWallet.walStatus,
            usiNum = updateDTO.usiNum ?: existingWallet.usiNum,
            astId = updateDTO.astId ?: existingWallet.astId,
            polId = updateDTO.polId ?: existingWallet.polId,
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime,
            active = updateDTO.active ?: existingWallet.active
        )
        
        return repository.updateWallet(walletToUpdate)
    }
    
    // 지갑 상태 업데이트
    suspend fun updateWalletStatus(walNum: Int, walStatus: String, userNum: Int): Boolean {
        return repository.updateWalletStatus(walNum, walStatus, userNum)
    }

    // 지갑 삭제 (비활성화)
    suspend fun deleteWallet(walNum: Int): Boolean {
        return repository.deleteWallet(walNum)
    }

    // 지갑 타입 유효성 검사
    fun validateWalletType(type: String): Boolean {
        return type in listOf("Self-custody Hot", "Cold", "Trading")
    }

    // 지갑 프로토콜 유효성 검사
    fun validateWalletProtocol(protocol: String): Boolean {
        return protocol in listOf("MPC", "Multisig")
    }
    
    // 지갑 상태 유효성 검사
    fun validateWalletStatus(status: String): Boolean {
        return status in listOf("frozen", "archived", "active")
    }
}

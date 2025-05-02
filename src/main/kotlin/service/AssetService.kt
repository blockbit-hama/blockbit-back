package com.sg.service

import com.sg.dto.AssetDTO
import com.sg.dto.AssetResponseDTO
import com.sg.dto.AssetUpdateDTO
import com.sg.dto.DateTimeUtil
import com.sg.repository.AssetRepository

class AssetService(private val repository: AssetRepository = AssetRepository()) {

    // 모든 자산 목록 조회
    suspend fun getAllAssets(): List<AssetResponseDTO> {
        return repository.getAllAssets()
    }

    // 자산 번호로 자산 정보 조회
    suspend fun getAssetByNum(num: Int): AssetResponseDTO? {
        return repository.getAssetByNum(num)
    }

    // 자산 심볼로 자산 정보 조회
    suspend fun getAssetBySymbol(symbol: String): AssetResponseDTO? {
        return repository.getAssetBySymbol(symbol)
    }

    // 자산 타입으로 자산 목록 조회
    suspend fun getAssetsByType(type: String): List<AssetResponseDTO> {
        return repository.getAssetsByType(type)
    }

    // 네트워크로 자산 목록 조회
    suspend fun getAssetsByNetwork(network: String): List<AssetResponseDTO> {
        return repository.getAssetsByNetwork(network)
    }

    // 새 자산 추가
    suspend fun addAsset(asset: AssetDTO, userNum: Int): Int {
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val assetWithTimestamp = asset.copy(
            creusr = userNum,
            credat = currentDate,
            cretim = currentTime,
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.addAsset(assetWithTimestamp)
    }

    // 자산 정보 업데이트
    suspend fun updateAsset(asset: AssetDTO, userNum: Int): Boolean {
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val assetWithTimestamp = asset.copy(
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.updateAsset(assetWithTimestamp)
    }

    // 자산 정보 부분 업데이트
    suspend fun partialUpdateAsset(updateDTO: AssetUpdateDTO, userNum: Int): Boolean {
        // 기존 자산 정보 조회
        val existingAsset = repository.getAssetByNum(updateDTO.astNum)
            ?: return false // 자산이 존재하지 않으면 업데이트 실패
        
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        // 업데이트할 필드만 변경하여 DTO 생성
        val assetToUpdate = AssetDTO(
            astNum = updateDTO.astNum,
            astName = updateDTO.astName ?: existingAsset.astName,
            astSymbol = updateDTO.astSymbol ?: existingAsset.astSymbol,
            astType = updateDTO.astType ?: existingAsset.astType,
            astNetwork = updateDTO.astNetwork ?: existingAsset.astNetwork,
            astDecimals = updateDTO.astDecimals ?: existingAsset.astDecimals,
            lmousr = userNum,  // 현재 사용자 번호 설정
            lmodat = currentDate,
            lmotim = currentTime,
            active = updateDTO.active ?: existingAsset.active
        )
        
        return repository.updateAsset(assetToUpdate)
    }

    // 자산 삭제 (비활성화)
    suspend fun deleteAsset(astNum: Int): Boolean {
        return repository.deleteAsset(astNum)
    }

    // 자산 타입 유효성 검사
    fun validateAssetType(type: String): Boolean {
        return type in listOf("coin", "token")
    }

    // 자산 네트워크 유효성 검사
    fun validateAssetNetwork(network: String): Boolean {
        return network in listOf("mainnet", "testnet")
    }
}

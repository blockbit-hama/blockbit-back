package com.sg.service

import com.sg.dto.AddressDTO
import com.sg.dto.AddressResponseDTO
import com.sg.dto.AddressUpdateDTO
import com.sg.dto.DateTimeUtil
import com.sg.repository.AddressRepository

class AddressService(private val repository: AddressRepository = AddressRepository()) {

    // 모든 주소 목록 조회
    suspend fun getAllAddresses(): List<AddressResponseDTO> {
        return repository.getAllAddresses()
    }

    // 주소 번호로 조회
    suspend fun getAddressByNum(num: Int): AddressResponseDTO? {
        return repository.getAddressByNum(num)
    }

    // 지갑 ID로 주소 목록 조회
    suspend fun getAddressesByWallet(walId: Int): List<AddressResponseDTO> {
        return repository.getAddressesByWallet(walId)
    }
    
    // 자산 ID로 주소 목록 조회
    suspend fun getAddressesByAsset(astId: Int): List<AddressResponseDTO> {
        return repository.getAddressesByAsset(astId)
    }
    
    // 주소 타입으로 조회
    suspend fun getAddressesByType(adrType: String): List<AddressResponseDTO> {
        return repository.getAddressesByType(adrType)
    }
    
    // 실제 암호화폐 주소로 조회
    suspend fun getAddressByAddress(adrAddress: String): AddressResponseDTO? {
        return repository.getAddressByAddress(adrAddress)
    }

    // 새 주소 추가
    suspend fun addAddress(address: AddressDTO, userNum: Int): Int {
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val addressWithTimestamp = address.copy(
            creusr = userNum,
            credat = currentDate,
            cretim = currentTime,
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.addAddress(addressWithTimestamp)
    }

    // 주소 정보 업데이트
    suspend fun updateAddress(address: AddressDTO, userNum: Int): Boolean {
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        val addressWithTimestamp = address.copy(
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime
        )
        
        return repository.updateAddress(addressWithTimestamp)
    }

    // 주소 정보 부분 업데이트
    suspend fun partialUpdateAddress(updateDTO: AddressUpdateDTO, userNum: Int): Boolean {
        // 기존 주소 정보 조회
        val existingAddress = repository.getAddressByNum(updateDTO.adrNum)
            ?: return false // 주소가 존재하지 않으면 업데이트 실패
        
        // 현재 날짜 및 시간 설정
        val currentDate = DateTimeUtil.getCurrentDate()
        val currentTime = DateTimeUtil.getCurrentTime()
        
        // 업데이트할 필드만 변경하여 DTO 생성
        val addressToUpdate = AddressDTO(
            adrNum = updateDTO.adrNum,
            adrAddress = existingAddress.adrAddress,
            adrLabel = updateDTO.adrLabel ?: existingAddress.adrLabel,
            adrType = updateDTO.adrType ?: existingAddress.adrType,
            adrPath = existingAddress.adrPath,
            walId = existingAddress.walId,
            astId = existingAddress.astId,
            lmousr = userNum,
            lmodat = currentDate,
            lmotim = currentTime,
            active = updateDTO.active ?: existingAddress.active
        )
        
        return repository.updateAddress(addressToUpdate)
    }

    // 주소 삭제 (비활성화)
    suspend fun deleteAddress(adrNum: Int): Boolean {
        return repository.deleteAddress(adrNum)
    }

    // 주소 타입 유효성 검사
    fun validateAddressType(type: String): Boolean {
        return type in listOf("receive", "change", "cold")
    }
}

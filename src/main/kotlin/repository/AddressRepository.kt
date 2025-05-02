package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.AddressDTO
import com.sg.dto.AddressResponseDTO
import org.jetbrains.exposed.sql.*

object AddressTable : Table("addresses") {
    val adrNum = integer("adr_num").autoIncrement()
    val adrAddress = varchar("adr_address", 255)
    val adrLabel = varchar("adr_label", 100).nullable()
    val adrType = varchar("adr_type", 20)
    val adrPath = varchar("adr_path", 100).nullable()
    val walId = integer("wal_id").references(WalletTable.walNum).nullable()
    val astId = integer("ast_id").references(AssetTable.astNum).nullable()
    val creusr = integer("creusr").nullable()
    val credat = varchar("credat", 8).nullable()
    val cretim = varchar("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = varchar("lmodat", 8).nullable()
    val lmotim = varchar("lmotim", 6).nullable()
    val active = varchar("active", 1)

    override val primaryKey = PrimaryKey(adrNum)
}

class AddressRepository {
    
    // 모든 주소 조회
    suspend fun getAllAddresses(): List<AddressResponseDTO> = dbQuery {
        AddressTable.selectAll()
            .map { resultRowToAddressResponseDTO(it) }
    }

    // 주소 번호로 조회
    suspend fun getAddressByNum(num: Int): AddressResponseDTO? = dbQuery {
        AddressTable
            .select { AddressTable.adrNum eq num }
            .map { resultRowToAddressResponseDTO(it) }
            .singleOrNull()
    }

    // 지갑 ID로 주소 목록 조회
    suspend fun getAddressesByWallet(walId: Int): List<AddressResponseDTO> = dbQuery {
        AddressTable
            .select { AddressTable.walId eq walId }
            .map { resultRowToAddressResponseDTO(it) }
    }
    
    // 자산 ID로 주소 목록 조회
    suspend fun getAddressesByAsset(astId: Int): List<AddressResponseDTO> = dbQuery {
        AddressTable
            .select { AddressTable.astId eq astId }
            .map { resultRowToAddressResponseDTO(it) }
    }
    
    // 주소 타입으로 조회
    suspend fun getAddressesByType(adrType: String): List<AddressResponseDTO> = dbQuery {
        AddressTable
            .select { AddressTable.adrType eq adrType }
            .map { resultRowToAddressResponseDTO(it) }
    }
    
    // 실제 암호화폐 주소로 조회
    suspend fun getAddressByAddress(adrAddress: String): AddressResponseDTO? = dbQuery {
        AddressTable
            .select { AddressTable.adrAddress eq adrAddress }
            .map { resultRowToAddressResponseDTO(it) }
            .singleOrNull()
    }

    // 주소 추가
    suspend fun addAddress(address: AddressDTO): Int = dbQuery {
        AddressTable.insert {
            it[adrAddress] = address.adrAddress
            it[adrLabel] = address.adrLabel
            it[adrType] = address.adrType
            it[adrPath] = address.adrPath
            it[walId] = address.walId
            it[astId] = address.astId
            it[creusr] = address.creusr
            it[credat] = address.credat
            it[cretim] = address.cretim
            it[lmousr] = address.lmousr
            it[lmodat] = address.lmodat
            it[lmotim] = address.lmotim
            it[active] = address.active
        }[AddressTable.adrNum]
    }

    // 주소 정보 업데이트
    suspend fun updateAddress(address: AddressDTO): Boolean = dbQuery {
        if (address.adrNum == null) return@dbQuery false
        
        val updateResult = AddressTable.update({ AddressTable.adrNum eq address.adrNum }) {
            it[adrAddress] = address.adrAddress
            it[adrLabel] = address.adrLabel
            it[adrType] = address.adrType
            it[adrPath] = address.adrPath
            it[walId] = address.walId
            it[astId] = address.astId
            it[lmousr] = address.lmousr
            it[lmodat] = address.lmodat
            it[lmotim] = address.lmotim
            it[active] = address.active
        }
        updateResult > 0
    }

    // 주소 부분 업데이트 (일부 필드만)
    suspend fun partialUpdateAddress(address: AddressDTO): Boolean = dbQuery {
        if (address.adrNum == null) return@dbQuery false
        
        // 수정할 필드 수집 (업데이트할 내용이 있는지 추적)
        var hasUpdates = false
        
        // 업데이트 실행
        val updateResult = AddressTable.update({ AddressTable.adrNum eq address.adrNum }) { 
            if (address.adrAddress != null) {
                it[adrAddress] = address.adrAddress
                hasUpdates = true
            }
            if (address.adrLabel != null) {
                it[adrLabel] = address.adrLabel
                hasUpdates = true
            }
            if (address.adrType != null) {
                it[adrType] = address.adrType
                hasUpdates = true
            }
            if (address.adrPath != null) {
                it[adrPath] = address.adrPath
                hasUpdates = true
            }
            if (address.walId != null) {
                it[walId] = address.walId
                hasUpdates = true
            }
            if (address.astId != null) {
                it[astId] = address.astId
                hasUpdates = true
            }
            if (address.lmousr != null) {
                it[lmousr] = address.lmousr
                hasUpdates = true
            }
            if (address.lmodat != null) {
                it[lmodat] = address.lmodat
                hasUpdates = true
            }
            if (address.lmotim != null) {
                it[lmotim] = address.lmotim
                hasUpdates = true
            }
            if (address.active != null) {
                it[active] = address.active
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

    // 주소 삭제 (비활성화)
    suspend fun deleteAddress(adrNum: Int): Boolean = dbQuery {
        val updateResult = AddressTable.update({ AddressTable.adrNum eq adrNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }

    // ResultRow를 응답 DTO로 변환하는 함수
    private fun resultRowToAddressResponseDTO(row: ResultRow) = AddressResponseDTO(
        adrNum = row[AddressTable.adrNum],
        adrAddress = row[AddressTable.adrAddress],
        adrLabel = row[AddressTable.adrLabel],
        adrType = row[AddressTable.adrType],
        adrPath = row[AddressTable.adrPath],
        walId = row[AddressTable.walId],
        astId = row[AddressTable.astId],
        active = row[AddressTable.active]
    )

    // ResultRow를 DTO로 변환하는 함수
    private fun resultRowToAddressDTO(row: ResultRow) = AddressDTO(
        adrNum = row[AddressTable.adrNum],
        adrAddress = row[AddressTable.adrAddress],
        adrLabel = row[AddressTable.adrLabel],
        adrType = row[AddressTable.adrType],
        adrPath = row[AddressTable.adrPath],
        walId = row[AddressTable.walId],
        astId = row[AddressTable.astId],
        creusr = row[AddressTable.creusr],
        credat = row[AddressTable.credat],
        cretim = row[AddressTable.cretim],
        lmousr = row[AddressTable.lmousr],
        lmodat = row[AddressTable.lmodat],
        lmotim = row[AddressTable.lmotim],
        active = row[AddressTable.active]
    )
}

package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.BalanceDTO
import com.sg.dto.BalanceResponseDTO
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal

object BalanceTable : Table("balances") {
    val balNum = integer("bal_num").autoIncrement()
    val adrId = integer("adr_id").references(AddressTable.adrNum)
    val astId = integer("ast_id").references(AssetTable.astNum)
    val balBefore = decimal("bal_before", 20, 8)
    val balAfter = decimal("bal_after", 20, 8)
    val balConfirmed = decimal("bal_confirmed", 20, 8)
    val balPending = decimal("bal_pending", 20, 8)
    val creusr = integer("creusr").nullable()
    val credat = char("credat", 8).nullable()
    val cretim = char("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = char("lmodat", 8).nullable()
    val lmotim = char("lmotim", 6).nullable()
    val active = char("active", 1)

    override val primaryKey = PrimaryKey(balNum)
}

class BalanceRepository {
    
    // 모든 잔액 정보 조회
    suspend fun getAllBalances(): List<BalanceResponseDTO> = dbQuery {
        BalanceTable.selectAll()
            .map { resultRowToBalanceResponseDTO(it) }
    }
    
    // 특정 주소의 잔액 정보 조회
    suspend fun getBalancesByAddressId(addressId: Int): List<BalanceResponseDTO> = dbQuery {
        BalanceTable
            .select { BalanceTable.adrId eq addressId }
            .map { resultRowToBalanceResponseDTO(it) }
    }
    
    // 특정 자산의 잔액 정보 조회
    suspend fun getBalancesByAssetId(assetId: Int): List<BalanceResponseDTO> = dbQuery {
        BalanceTable
            .select { BalanceTable.astId eq assetId }
            .map { resultRowToBalanceResponseDTO(it) }
    }
    
    // 특정 잔액 정보 조회
    suspend fun getBalanceById(balNum: Int): BalanceResponseDTO? = dbQuery {
        BalanceTable
            .select { BalanceTable.balNum eq balNum }
            .map { resultRowToBalanceResponseDTO(it) }
            .singleOrNull()
    }
    
    // 잔액 정보 추가
    suspend fun addBalance(balance: BalanceDTO): Int = dbQuery {
        val adrId = balance.adrId ?: error("Address ID is required")
        val astId = balance.astId ?: error("Asset ID is required")
        
        BalanceTable.insert {
            it[BalanceTable.adrId] = adrId
            it[BalanceTable.astId] = astId
            it[BalanceTable.balBefore] = BigDecimal.valueOf(balance.balBefore ?: 0.0)
            it[BalanceTable.balAfter] = BigDecimal.valueOf(balance.balAfter ?: 0.0)
            it[BalanceTable.balConfirmed] = BigDecimal.valueOf(balance.balConfirmed ?: 0.0)
            it[BalanceTable.balPending] = BigDecimal.valueOf(balance.balPending ?: 0.0)
            it[BalanceTable.creusr] = balance.creusr
            it[BalanceTable.credat] = balance.credat
            it[BalanceTable.cretim] = balance.cretim
            it[BalanceTable.lmousr] = balance.lmousr
            it[BalanceTable.lmodat] = balance.lmodat
            it[BalanceTable.lmotim] = balance.lmotim
            it[BalanceTable.active] = balance.active
        }[BalanceTable.balNum]
    }
    
    // 잔액 정보 업데이트
    suspend fun updateBalance(balance: BalanceDTO): Boolean = dbQuery {
        if (balance.balNum == null) return@dbQuery false
        
        // updateBuilder 블록 내에서 사용할 변수들을 먼저, 명시적으로 타입을 지정하여 선언
        val balBeforeBD = balance.balBefore?.let { BigDecimal.valueOf(it) }
        val balAfterBD = balance.balAfter?.let { BigDecimal.valueOf(it) }
        val balConfirmedBD = balance.balConfirmed?.let { BigDecimal.valueOf(it) }
        val balPendingBD = balance.balPending?.let { BigDecimal.valueOf(it) }
        
        val updateResult = BalanceTable.update({ BalanceTable.balNum eq balance.balNum }) { stmt ->
            balance.adrId?.let { stmt[BalanceTable.adrId] = it }
            balance.astId?.let { stmt[BalanceTable.astId] = it }
            balance.creusr?.let { stmt[BalanceTable.creusr] = it }
            balance.credat?.let { stmt[BalanceTable.credat] = it }
            balance.cretim?.let { stmt[BalanceTable.cretim] = it }
            balance.lmousr?.let { stmt[BalanceTable.lmousr] = it }
            balance.lmodat?.let { stmt[BalanceTable.lmodat] = it }
            balance.lmotim?.let { stmt[BalanceTable.lmotim] = it }
            balance.active?.let { stmt[BalanceTable.active] = it }
            
            // 미리 변환한 BigDecimal 값을 사용
            balBeforeBD?.let { stmt[BalanceTable.balBefore] = it }
            balAfterBD?.let { stmt[BalanceTable.balAfter] = it }
            balConfirmedBD?.let { stmt[BalanceTable.balConfirmed] = it }
            balPendingBD?.let { stmt[BalanceTable.balPending] = it }
        }
        updateResult > 0
    }
    
    // 잔액 정보 삭제 (비활성화)
    suspend fun deleteBalance(balNum: Int): Boolean = dbQuery {
        val updateResult = BalanceTable.update({ BalanceTable.balNum eq balNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }
    
    // ResultRow를 DTO로 변환하는 함수
    private fun resultRowToBalanceDTO(row: ResultRow) = BalanceDTO(
        balNum = row[BalanceTable.balNum],
        adrId = row[BalanceTable.adrId],
        astId = row[BalanceTable.astId],
        balBefore = row[BalanceTable.balBefore].toDouble(),
        balAfter = row[BalanceTable.balAfter].toDouble(),
        balConfirmed = row[BalanceTable.balConfirmed].toDouble(),
        balPending = row[BalanceTable.balPending].toDouble(),
        creusr = row[BalanceTable.creusr],
        credat = row[BalanceTable.credat],
        cretim = row[BalanceTable.cretim],
        lmousr = row[BalanceTable.lmousr],
        lmodat = row[BalanceTable.lmodat],
        lmotim = row[BalanceTable.lmotim],
        active = row[BalanceTable.active]
    )
    
    // ResultRow를 응답 DTO로 변환하는 함수
    private fun resultRowToBalanceResponseDTO(row: ResultRow) = BalanceResponseDTO(
        balNum = row[BalanceTable.balNum],
        adrId = row[BalanceTable.adrId],
        astId = row[BalanceTable.astId],
        balBefore = row[BalanceTable.balBefore].toDouble(),
        balAfter = row[BalanceTable.balAfter].toDouble(),
        balConfirmed = row[BalanceTable.balConfirmed].toDouble(),
        balPending = row[BalanceTable.balPending].toDouble(),
        creusr = row[BalanceTable.creusr],
        credat = row[BalanceTable.credat],
        cretim = row[BalanceTable.cretim],
        lmousr = row[BalanceTable.lmousr],
        lmodat = row[BalanceTable.lmodat],
        lmotim = row[BalanceTable.lmotim],
        active = row[BalanceTable.active]
    )
}

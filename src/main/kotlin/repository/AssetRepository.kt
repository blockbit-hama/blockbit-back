package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.AssetDTO
import com.sg.dto.AssetResponseDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement

object AssetTable : Table("assets") {
    val astNum = integer("ast_num").autoIncrement()
    val astName = varchar("ast_name", 100)
    val astSymbol = varchar("ast_symbol", 20)
    val astType = varchar("ast_type", 20)
    val astNetwork = varchar("ast_network", 20)
    val astDecimals = integer("ast_decimals").nullable()
    val creusr = integer("creusr").nullable()
    val credat = varchar("credat", 8).nullable()
    val cretim = varchar("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = varchar("lmodat", 8).nullable()
    val lmotim = varchar("lmotim", 6).nullable()
    val active = varchar("active", 1)

    override val primaryKey = PrimaryKey(astNum)
}

class AssetRepository {
    
    // 모든 자산 조회
    suspend fun getAllAssets(): List<AssetResponseDTO> = dbQuery {
        AssetTable.selectAll()
            .map { resultRowToAssetResponseDTO(it) }
    }

    // 자산 번호로 자산 정보 조회
    suspend fun getAssetByNum(num: Int): AssetResponseDTO? = dbQuery {
        AssetTable
            .select { AssetTable.astNum eq num }
            .map { resultRowToAssetResponseDTO(it) }
            .singleOrNull()
    }

    // 자산 심볼로 자산 정보 조회
    suspend fun getAssetBySymbol(symbol: String): AssetResponseDTO? = dbQuery {
        AssetTable
            .select { AssetTable.astSymbol eq symbol }
            .map { resultRowToAssetResponseDTO(it) }
            .singleOrNull()
    }

    // 자산 타입으로 자산 목록 조회
    suspend fun getAssetsByType(type: String): List<AssetResponseDTO> = dbQuery {
        AssetTable
            .select { AssetTable.astType eq type }
            .map { resultRowToAssetResponseDTO(it) }
    }

    // 네트워크로 자산 목록 조회
    suspend fun getAssetsByNetwork(network: String): List<AssetResponseDTO> = dbQuery {
        AssetTable
            .select { AssetTable.astNetwork eq network }
            .map { resultRowToAssetResponseDTO(it) }
    }

    // 자산 추가
    suspend fun addAsset(asset: AssetDTO): Int = dbQuery {
        AssetTable.insert {
            it[astName] = asset.astName
            it[astSymbol] = asset.astSymbol
            it[astType] = asset.astType
            it[astNetwork] = asset.astNetwork
            it[astDecimals] = asset.astDecimals
            it[creusr] = asset.creusr
            it[credat] = asset.credat
            it[cretim] = asset.cretim
            it[lmousr] = asset.lmousr
            it[lmodat] = asset.lmodat
            it[lmotim] = asset.lmotim
            it[active] = asset.active
        }[AssetTable.astNum]
    }

    // 자산 정보 업데이트
    suspend fun updateAsset(asset: AssetDTO): Boolean = dbQuery {
        if (asset.astNum == null) return@dbQuery false
        
        val updateResult = AssetTable.update({ AssetTable.astNum eq asset.astNum }) {
            it[astName] = asset.astName
            it[astSymbol] = asset.astSymbol
            it[astType] = asset.astType
            it[astNetwork] = asset.astNetwork
            it[astDecimals] = asset.astDecimals
            it[lmousr] = asset.lmousr
            it[lmodat] = asset.lmodat
            it[lmotim] = asset.lmotim
            it[active] = asset.active
        }
        updateResult > 0
    }

    // 자산 부분 업데이트 (일부 필드만)
    suspend fun partialUpdateAsset(asset: AssetDTO): Boolean = dbQuery {
        if (asset.astNum == null) return@dbQuery false
        
        val updateResult = AssetTable.update({ AssetTable.astNum eq asset.astNum }) {
            asset.astName?.let { name -> it[astName] = name }
            asset.astSymbol?.let { symbol -> it[astSymbol] = symbol }
            asset.astType?.let { type -> it[astType] = type }
            asset.astNetwork?.let { network -> it[astNetwork] = network }
            asset.astDecimals?.let { decimals -> it[astDecimals] = decimals }
            asset.lmousr?.let { modUsr -> it[lmousr] = modUsr }
            asset.lmodat?.let { modDat -> it[lmodat] = modDat }
            asset.lmotim?.let { modTim -> it[lmotim] = modTim }
            it[active] = asset.active
        }
        updateResult > 0
    }

    // 자산 삭제 (실제 삭제가 아닌 active 상태 변경)
    suspend fun deleteAsset(astNum: Int): Boolean = dbQuery {
        val updateResult = AssetTable.update({ AssetTable.astNum eq astNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }

    // ResultRow를 AssetResponseDTO로 변환하는 함수
    private fun resultRowToAssetResponseDTO(row: ResultRow) = AssetResponseDTO(
        astNum = row[AssetTable.astNum],
        astName = row[AssetTable.astName],
        astSymbol = row[AssetTable.astSymbol],
        astType = row[AssetTable.astType],
        astNetwork = row[AssetTable.astNetwork],
        astDecimals = row[AssetTable.astDecimals],
        active = row[AssetTable.active]
    )

    // ResultRow를 AssetDTO로 변환하는 함수
    private fun resultRowToAssetDTO(row: ResultRow) = AssetDTO(
        astNum = row[AssetTable.astNum],
        astName = row[AssetTable.astName],
        astSymbol = row[AssetTable.astSymbol],
        astType = row[AssetTable.astType],
        astNetwork = row[AssetTable.astNetwork],
        astDecimals = row[AssetTable.astDecimals],
        creusr = row[AssetTable.creusr],
        credat = row[AssetTable.credat],
        cretim = row[AssetTable.cretim],
        lmousr = row[AssetTable.lmousr],
        lmodat = row[AssetTable.lmodat],
        lmotim = row[AssetTable.lmotim],
        active = row[AssetTable.active]
    )
}

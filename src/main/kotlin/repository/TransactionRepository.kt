package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.TransactionDTO
import com.sg.dto.TransactionResponseDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object TransactionTable : Table("transactions") {
    val trxNum = integer("trx_num").autoIncrement()
    val trxHash = varchar("trx_hash", 255)
    val trxType = varchar("trx_type", 20)
    val trxAmount = decimal("trx_amount", 20, 8)
    val trxFee = decimal("trx_fee", 20, 8).nullable()
    val trxStatus = varchar("trx_status", 20)
    val trxConfirmedAt = datetime("trx_confirmed_at").nullable()
    val trxMemo = text("trx_memo").nullable()
    val walId = integer("wal_id").references(WalletTable.walNum)
    val astId = integer("ast_id").references(AssetTable.astNum)
    val creusr = integer("creusr").nullable()
    val credat = char("credat", 8).nullable()
    val cretim = char("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = char("lmodat", 8).nullable()
    val lmotim = char("lmotim", 6).nullable()
    val active = char("active", 1)

    override val primaryKey = PrimaryKey(trxNum)
}

class TransactionRepository {
    
    // 모든 트랜잭션 조회
    suspend fun getAllTransactions(): List<TransactionResponseDTO> = dbQuery {
        val assetJoin = TransactionTable.join(AssetTable, JoinType.LEFT,
            additionalConstraint = { TransactionTable.astId eq AssetTable.astNum })
            .join(WalletTable, JoinType.LEFT,
                additionalConstraint = { TransactionTable.walId eq WalletTable.walNum })
        
        assetJoin.selectAll()
            .map { resultRowToTransactionResponseDTO(it) }
    }
    
    // 지갑 ID로 트랜잭션 조회
    suspend fun getTransactionsByWalletId(walletId: Int): List<TransactionResponseDTO> = dbQuery {
        val assetJoin = TransactionTable.join(AssetTable, JoinType.LEFT,
            additionalConstraint = { TransactionTable.astId eq AssetTable.astNum })
            .join(WalletTable, JoinType.LEFT,
                additionalConstraint = { TransactionTable.walId eq WalletTable.walNum })
        
        assetJoin.select { TransactionTable.walId eq walletId }
            .map { resultRowToTransactionResponseDTO(it) }
    }
    
    // 특정 트랜잭션 조회
    suspend fun getTransactionById(trxNum: Int): TransactionResponseDTO? = dbQuery {
        val assetJoin = TransactionTable.join(AssetTable, JoinType.LEFT,
            additionalConstraint = { TransactionTable.astId eq AssetTable.astNum })
            .join(WalletTable, JoinType.LEFT,
                additionalConstraint = { TransactionTable.walId eq WalletTable.walNum })
        
        assetJoin.select { TransactionTable.trxNum eq trxNum }
            .map { resultRowToTransactionResponseDTO(it) }
            .singleOrNull()
    }
    
    // 트랜잭션 추가
    suspend fun addTransaction(transaction: TransactionDTO): Int = dbQuery {
        val trxHash = transaction.trxHash ?: error("Transaction hash is required")
        val trxType = transaction.trxType ?: error("Transaction type is required")
        val trxAmount = transaction.trxAmount ?: error("Transaction amount is required")
        val walId = transaction.walId ?: error("Wallet ID is required")
        val astId = transaction.astId ?: error("Asset ID is required")
        
        TransactionTable.insert {
            it[TransactionTable.trxHash] = trxHash
            it[TransactionTable.trxType] = trxType
            it[TransactionTable.trxAmount] = BigDecimal.valueOf(trxAmount)
            if (transaction.trxFee != null) {
                it[TransactionTable.trxFee] = BigDecimal.valueOf(transaction.trxFee)
            }
            it[TransactionTable.trxStatus] = transaction.trxStatus ?: "pending"
            it[TransactionTable.trxConfirmedAt] = transaction.trxConfirmedAt
            it[TransactionTable.trxMemo] = transaction.trxMemo
            it[TransactionTable.walId] = walId
            it[TransactionTable.astId] = astId
            it[TransactionTable.creusr] = transaction.creusr
            it[TransactionTable.credat] = transaction.credat
            it[TransactionTable.cretim] = transaction.cretim
            it[TransactionTable.lmousr] = transaction.lmousr
            it[TransactionTable.lmodat] = transaction.lmodat
            it[TransactionTable.lmotim] = transaction.lmotim
            it[TransactionTable.active] = transaction.active
        }[TransactionTable.trxNum]
    }
    
    // 트랜잭션 업데이트
    suspend fun updateTransaction(transaction: TransactionDTO): Boolean = dbQuery {
        if (transaction.trxNum == null) return@dbQuery false

        val trxAmountBD = transaction.trxAmount?.let { BigDecimal.valueOf(it) }
        val trxFeeBD = transaction.trxFee?.let { BigDecimal.valueOf(it) }
        
        val updateResult = TransactionTable.update({ TransactionTable.trxNum eq transaction.trxNum }) { stmt ->
            transaction.trxHash?.let { stmt[TransactionTable.trxHash] = it }
            transaction.trxType?.let { stmt[TransactionTable.trxType] = it }
            transaction.trxStatus?.let { stmt[TransactionTable.trxStatus] = it }
            transaction.trxConfirmedAt?.let { stmt[TransactionTable.trxConfirmedAt] = it }
            transaction.trxMemo?.let { stmt[TransactionTable.trxMemo] = it }
            transaction.walId?.let { stmt[TransactionTable.walId] = it }
            transaction.astId?.let { stmt[TransactionTable.astId] = it }
            transaction.creusr?.let { stmt[TransactionTable.creusr] = it }
            transaction.credat?.let { stmt[TransactionTable.credat] = it }
            transaction.cretim?.let { stmt[TransactionTable.cretim] = it }
            transaction.lmousr?.let { stmt[TransactionTable.lmousr] = it }
            transaction.lmodat?.let { stmt[TransactionTable.lmodat] = it }
            transaction.lmotim?.let { stmt[TransactionTable.lmotim] = it }
            transaction.active?.let { stmt[TransactionTable.active] = it }
            
            // 미리 변환한 BigDecimal 값을 사용
            trxAmountBD?.let { stmt[TransactionTable.trxAmount] = it }
            trxFeeBD?.let { stmt[TransactionTable.trxFee] = it }
        }
        updateResult > 0
    }
    
    // 트랜잭션 상태 업데이트
    suspend fun updateTransactionStatus(trxNum: Int, status: String, confirmedAt: LocalDateTime? = null): Boolean = dbQuery {
        val updateResult = TransactionTable.update({ TransactionTable.trxNum eq trxNum }) {
            it[trxStatus] = status
            if (status == "confirmed" && confirmedAt != null) {
                it[trxConfirmedAt] = confirmedAt
            }
        }
        updateResult > 0
    }
    
    // 트랜잭션 삭제 (비활성화)
    suspend fun deleteTransaction(trxNum: Int): Boolean = dbQuery {
        val updateResult = TransactionTable.update({ TransactionTable.trxNum eq trxNum }) {
            it[active] = "0"
        }
        updateResult > 0
    }
    
    // ResultRow를 DTO로 변환하는 함수
    private fun resultRowToTransactionDTO(row: ResultRow) = TransactionDTO(
        trxNum = row[TransactionTable.trxNum],
        trxHash = row[TransactionTable.trxHash],
        trxType = row[TransactionTable.trxType],
        trxAmount = row[TransactionTable.trxAmount].toDouble(),
        trxFee = row[TransactionTable.trxFee]?.toDouble(),
        trxStatus = row[TransactionTable.trxStatus],
        trxConfirmedAt = row[TransactionTable.trxConfirmedAt],
        trxMemo = row[TransactionTable.trxMemo],
        walId = row[TransactionTable.walId],
        astId = row[TransactionTable.astId],
        creusr = row[TransactionTable.creusr],
        credat = row[TransactionTable.credat],
        cretim = row[TransactionTable.cretim],
        lmousr = row[TransactionTable.lmousr],
        lmodat = row[TransactionTable.lmodat],
        lmotim = row[TransactionTable.lmotim],
        active = row[TransactionTable.active]
    )
    
    // ResultRow를 응답 DTO로 변환하는 함수 (자산 심볼과 지갑 이름 포함)
    private fun resultRowToTransactionResponseDTO(row: ResultRow) = TransactionResponseDTO(
        trxNum = row[TransactionTable.trxNum],
        trxHash = row[TransactionTable.trxHash],
        trxType = row[TransactionTable.trxType],
        trxAmount = row[TransactionTable.trxAmount].toDouble(),
        trxFee = row[TransactionTable.trxFee]?.toDouble(),
        trxStatus = row[TransactionTable.trxStatus],
        trxConfirmedAt = row[TransactionTable.trxConfirmedAt],
        trxMemo = row[TransactionTable.trxMemo],
        walId = row[TransactionTable.walId],
        astId = row[TransactionTable.astId],
        walletName = if (row.hasValue(WalletTable.walName)) row[WalletTable.walName] else null,
        assetSymbol = if (row.hasValue(AssetTable.astSymbol)) row[AssetTable.astSymbol] else null,
        active = row[TransactionTable.active]
    )
}

package com.sg.repository

import com.sg.config.factory.DatabaseFactory.dbQuery
import com.sg.dto.wallet.KeyShareDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

// Exposed 테이블 정의
object KeyShareTable : Table("key_shares") {
    val kshNum = integer("ksh_num").autoIncrement()
    val kshWalletId = varchar("ksh_wallet_id", 100)
    val kshParticipantIndex = integer("ksh_participant_index")
    val kshShareX = integer("ksh_share_x")
    val kshShareY = text("ksh_share_y")
    val kshPublicKey = text("ksh_public_key").nullable()
    val walNum = integer("wal_num").nullable()
    val creusr = integer("creusr").nullable()
    val credat = char("credat", 8).nullable()
    val cretim = char("cretim", 6).nullable()
    val lmousr = integer("lmousr").nullable()
    val lmodat = char("lmodat", 8).nullable()
    val lmotim = char("lmotim", 6).nullable()
    val active = char("active", 1).default("1")

    override val primaryKey = PrimaryKey(kshNum)
}

class KeyShareRepository {
    private val logger = LoggerFactory.getLogger(KeyShareRepository::class.java)

    // 키 공유 저장
    suspend fun saveKeyShare(keyShare: KeyShareDTO): Int = dbQuery {
        KeyShareTable.insert {
            it[kshWalletId] = keyShare.walletId
            it[kshParticipantIndex] = keyShare.participantIndex
            it[kshShareX] = keyShare.shareX
            it[kshShareY] = keyShare.shareY
            keyShare.publicKey?.let { pk -> it[kshPublicKey] = pk }
            keyShare.walNum?.let { wn -> it[walNum] = wn }
            keyShare.creusr?.let { cu -> it[creusr] = cu }
            keyShare.credat?.let { cd -> it[credat] = cd }
            keyShare.cretim?.let { ct -> it[cretim] = ct }
            keyShare.lmousr?.let { lu -> it[lmousr] = lu }
            keyShare.lmodat?.let { ld -> it[lmodat] = ld }
            keyShare.lmotim?.let { lt -> it[lmotim] = lt }
            it[active] = keyShare.active
        } get KeyShareTable.kshNum
    }

    // 지갑 ID로 모든 키 공유 조회
    suspend fun getKeySharesByWalletId(walletId: String): List<KeyShareDTO> = dbQuery {
        KeyShareTable
            .select { (KeyShareTable.kshWalletId eq walletId) and (KeyShareTable.active eq "1") }
            .map { rowToKeyShareDTO(it) }
    }

    // 특정 참여자의 키 공유 조회
    suspend fun getKeyShare(walletId: String, participantIndex: Int): KeyShareDTO? = dbQuery {
        KeyShareTable
            .select {
                (KeyShareTable.kshWalletId eq walletId) and
                        (KeyShareTable.kshParticipantIndex eq participantIndex) and
                        (KeyShareTable.active eq "1")
            }
            .map { rowToKeyShareDTO(it) }
            .singleOrNull()
    }

    // 키 공유 삭제 (soft delete)
    suspend fun deleteKeyShare(walletId: String, participantIndex: Int): Boolean = dbQuery {
        KeyShareTable.update({
            (KeyShareTable.kshWalletId eq walletId) and
                    (KeyShareTable.kshParticipantIndex eq participantIndex)
        }) {
            it[active] = "0"
        } > 0
    }

    // Row를 DTO로 변환
    private fun rowToKeyShareDTO(row: ResultRow): KeyShareDTO {
        return KeyShareDTO(
            kshNum = row[KeyShareTable.kshNum],
            walletId = row[KeyShareTable.kshWalletId],
            participantIndex = row[KeyShareTable.kshParticipantIndex],
            shareX = row[KeyShareTable.kshShareX],
            shareY = row[KeyShareTable.kshShareY],
            publicKey = row[KeyShareTable.kshPublicKey],
            walNum = row[KeyShareTable.walNum],
            creusr = row[KeyShareTable.creusr],
            credat = row[KeyShareTable.credat],
            cretim = row[KeyShareTable.cretim],
            lmousr = row[KeyShareTable.lmousr],
            lmodat = row[KeyShareTable.lmodat],
            lmotim = row[KeyShareTable.lmotim],
            active = row[KeyShareTable.active]
        )
    }
}
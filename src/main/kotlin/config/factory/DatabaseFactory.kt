package com.sg.config.factory

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(environment: ApplicationEnvironment) {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()
        val driver = environment.config.property("postgres.driver").getString()

        val database = Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )

        // 트랜잭션 내에서 테이블 스키마 생성 (필요한 경우)
        transaction(database) {
            // 테이블이 없으면 생성 (실제 운영 환경에서는 필요에 따라 비활성화)
            // SchemaUtils.create(UserInfoTable)
        }
    }

    // 코루틴 컨텍스트에서 DB 쿼리 실행하기 위한 유틸리티 함수
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

package com.sg.service.wallet.bitcoin

import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bitcoinj.core.*
import org.slf4j.LoggerFactory
import java.math.BigInteger

/**
 * BlockCypher API를 사용하여 비트코인 테스트넷과 통신하는 클라이언트
 */
class BlockCypherClient(
    private val apiBaseUrl: String = "https://api.blockcypher.com/v1/btc/test3",
    private val apiKey: String = ""
) {
    private val logger = LoggerFactory.getLogger(BlockCypherClient::class.java)
    private val params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * 주소의 UTXO 목록을 조회합니다.
     * @param address 조회할 주소
     * @return UTXO 목록 (트랜잭션 ID, 출력 인덱스, 금액)
     */
    fun getUTXOs(address: String): List<UTXOInfo> = runBlocking {
        try {
            // BlockCypher API 호출
            val apiUrl = "$apiBaseUrl/addrs/$address?unspentOnly=true&includeScript=true"
            val apiUrlWithKey = if (apiKey.isNotEmpty()) "$apiUrl&token=$apiKey" else apiUrl
            
            val response = client.get(apiUrlWithKey)
            val responseText = response.bodyAsString()
            val jsonObject = JsonParser.parseString(responseText).asJsonObject
            
            // JSON 파싱
            val utxos = mutableListOf<UTXOInfo>()
            val txRefs = jsonObject.getAsJsonArray("txrefs") ?: return@runBlocking emptyList()
            
            for (i in 0 until txRefs.size()) {
                val txRef = txRefs.get(i).asJsonObject
                val txHash = txRef.get("tx_hash").asString
                val outputIndex = txRef.get("tx_output_n").asInt
                val value = txRef.get("value").asLong
                val script = txRef.get("script").asString
                
                utxos.add(UTXOInfo(
                    txId = txHash,
                    outputIndex = outputIndex,
                    value = value,
                    script = script
                ))
            }
            
            return@runBlocking utxos
        } catch (e: Exception) {
            logger.error("UTXO 조회 실패", e)
            return@runBlocking emptyList()
        }
    }

    /**
     * 트랜잭션을 네트워크에 브로드캐스트합니다.
     * @param txHex 트랜잭션 16진수 문자열
     * @return 트랜잭션 해시 (성공 시) 또는 null (실패 시)
     */
    fun broadcastTransaction(txHex: String): String? = runBlocking {
        try {
            // BlockCypher API 호출
            val apiUrl = "$apiBaseUrl/txs/push"
            val apiUrlWithKey = if (apiKey.isNotEmpty()) "$apiUrl?token=$apiKey" else apiUrl
            
            val requestBody = """{"tx": "$txHex"}"""
            val response = client.post(apiUrlWithKey) {
                header("Content-Type", "application/json")
                setBody(requestBody)
            }
            val responseText = response.bodyAsString()
            
            // JSON 파싱
            val jsonObject = JsonParser.parseString(responseText).asJsonObject
            return@runBlocking jsonObject.get("tx").asJsonObject.get("hash").asString
        } catch (e: Exception) {
            logger.error("트랜잭션 브로드캐스트 실패", e)
            return@runBlocking null
        }
    }

    /**
     * 트랜잭션 수수료 추천 값을 조회합니다.
     * @return 추천 수수료 (satoshi/byte)
     */
    fun getRecommendedFee(): Int = runBlocking {
        try {
            // BlockCypher API 호출
            val apiUrl = "$apiBaseUrl"
            val apiUrlWithKey = if (apiKey.isNotEmpty()) "$apiUrl?token=$apiKey" else apiUrl
            
            val response = client.get(apiUrlWithKey)
            val responseText = response.bodyAsString()
            val jsonObject = JsonParser.parseString(responseText).asJsonObject
            
            // 일반 우선순위 수수료 반환 (중간값)
            return@runBlocking jsonObject.get("medium_fee_per_kb").asInt / 1000
        } catch (e: Exception) {
            logger.error("수수료 조회 실패", e)
            return@runBlocking 10 // 기본값 10 satoshi/byte
        }
    }
    
    /**
     * HTTP 응답 본문을 문자열로 변환하는 확장 함수
     */
    private suspend fun HttpResponse.bodyAsString(): String {
        return this.bodyAsText()
    }
}

/**
 * UTXO 정보를 담는 데이터 클래스
 */
data class UTXOInfo(
    val txId: String,          // 트랜잭션 ID
    val outputIndex: Int,      // 출력 인덱스
    val value: Long,           // 금액 (satoshi)
    val script: String         // 스크립트 (16진수)
)
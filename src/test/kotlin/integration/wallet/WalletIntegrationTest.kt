package integration.wallet

import com.sg.controller.wallet.walletRoutes
import com.sg.dto.wallet.*
import com.sg.service.wallet.BitcoinMultiSigService
import com.sg.service.wallet.EthereumMpcService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import kotlin.test.*

class WalletIntegrationTest {

    // 테스트용 변수들
    private lateinit var bitcoinWallet: MultisigWalletDTO
    private lateinit var ethereumWallet: MpcWalletDTO
    private lateinit var partialBtcTx: PartiallySignedTransactionDTO
    private lateinit var partialEthSig: PartialSignatureDTO
    private lateinit var completedBtcTx: String
    private lateinit var completedEthTxHash: String

    // 테스트 애플리케이션 설정
    private fun setupTestApplication(test: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            // 실제 서비스 초기화
            val bitcoinMultiSigService = BitcoinMultiSigService()
            val ethereumMpcService = EthereumMpcService()

            // 컨텐츠 네고시에이션 설정
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            // 라우팅 설정
            routing {
                walletRoutes(bitcoinMultiSigService, ethereumMpcService)
            }
        }

        // 클라이언트 설정
        // client.config {
        //     install(ContentNegotiation) {
        //         json(Json {
        //             prettyPrint = true
        //             isLenient = true
        //             ignoreUnknownKeys = true
        //         })
        //     }
        // }

        test()
    }

    @Test
    fun testWalletIntegration() = setupTestApplication {
        /*
         * 테스트 1: REST API를 통해 비트코인 멀티시그 지갑이 정상적으로 생성되는지 검증
         * 1. API 응답 상태 코드가 200 OK
         * 2. 응답 바디가 존재하는지
         * 3. 지갑 주소가 생성되었는지
         * 4. 리딤 스크립트가 생성되었는지
         */
        run {
            val response = client.post("/api/wallet/bitcoin/create")
            assertEquals(HttpStatusCode.Created, response.status)
            val responseText = response.bodyAsText()
            assertNotNull(responseText)

            bitcoinWallet = Json.decodeFromString(responseText)
            assertNotNull(bitcoinWallet.address)
            assertNotNull(bitcoinWallet.redeemScript)
            assertTrue(bitcoinWallet.publicKeys.size == 3)
            assertTrue(bitcoinWallet.privateKeys.size == 3)
        }

        /*
         * 테스트 2: REST API를 통해 이더리움 MPC 지갑이 정상적으로 생성되는지 검증
         * 1. API 응답 상태 코드가 200 OK
         * 2. 응답 바디가 존재하는지
         * 3. 지갑 ID가 생성되었는지
         * 4. 이더리움 주소가 생성되었는지
         */
        run {
            val response = client.post("/api/wallet/ethereum/create")
            assertEquals(HttpStatusCode.Created, response.status)
            val responseText = response.bodyAsText()
            assertNotNull(responseText)

            ethereumWallet = Json.decodeFromString(responseText)
            assertNotNull(ethereumWallet.walletId)
            assertNotNull(ethereumWallet.address)
        }

        /*
         * 테스트 3: REST API를 통해 비트코인 멀티시그 지갑의 첫 번째 서명이 정상적으로 생성되는지 검증
         * 1. 이전 테스트에서 지갑이 정상적으로 생성되었는지 확인
         * 2. API 응답 상태 코드가 200 OK인지
         * 3. 부분 서명된 트랜잭션이 생성되었는지
         * 4. 트랜잭션 데이터와 서명이 포함되었는지
         */
        run {
            assertNotNull(bitcoinWallet, "비트코인 지갑이 먼저 생성되어야 합니다")

            val request = BitcoinTransactionRequestDTO().apply {
                fromAddress = bitcoinWallet.address
                toAddress = "mwCwTceJvYV27KXBc3NJZys6CjsgsoeHmf" // 테스트넷 주소
                amountSatoshi = 10000L
                redeemScriptHex = bitcoinWallet.redeemScript
                privateKeyHex = bitcoinWallet.privateKeys[0] // 첫 번째 키로 서명
            }

            val response = client.post("/api/wallet/bitcoin/transaction/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseText = response.bodyAsText()
            assertNotNull(responseText)

            partialBtcTx = Json.decodeFromString(responseText)
            assertNotNull(partialBtcTx.transactionHex)
            assertNotNull(partialBtcTx.signatureHex)
        }

        /*
         * 테스트 4: REST API를 통해 이더리움 MPC 지갑의 첫 번째 부분 서명이 정상적으로 생성되는지 검증
         * 1. 이전 테스트에서 지갑이 정상적으로 생성되었는지 확인
         * 2. API 응답 상태 코드가 200 OK인지
         * 3. 부분 서명 정보가 생성되었는지
         * 4. 트랜잭션 해시와 부분 서명 데이터가 포함되었는지
         */
        run {
            assertNotNull(ethereumWallet, "이더리움 지갑이 먼저 생성되어야 합니다")

            val request = EthereumTransactionRequestDTO().apply {
                walletId = ethereumWallet.walletId
                participantIndex = 0 // 첫 번째 참여자
                toAddress = "0x9876543210987654321098765432109876543210"
                amount = "0.01" // 0.01 ETH
            }

            val response = client.post("/api/wallet/ethereum/transaction/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseText = response.bodyAsText()
            assertNotNull(responseText)

            partialEthSig = Json.decodeFromString(responseText)
            assertNotNull(partialEthSig.transactionHash)
            assertNotNull(partialEthSig.partialSignature)
        }

        /*
         * 테스트 5: REST API를 통해 비트코인 멀티시그 트랜잭션의 두 번째 서명 추가 및 완성이 정상적으로 이루어지는지 검증
         * 1. 이전 테스트들이 정상적으로 완료되었는지 확인
         * 2. API 응답 상태 코드가 200 OK인지
         * 3. 완성된 트랜잭션 문자열이 반환되었는지
         * 4. 트랜잭션 문자열이 유효한지(빈 문자열이 아닌지)
         */
        run {
            assertNotNull(partialBtcTx, "부분 서명된 비트코인 트랜잭션이 먼저 생성되어야 합니다")
            assertNotNull(bitcoinWallet, "비트코인 지갑이 먼저 생성되어야 합니다")

            val request = BitcoinCompleteRequestDTO().apply {
                partiallySignedTransaction = partialBtcTx
                privateKeyHex = bitcoinWallet.privateKeys[1] // 두 번째 키로 서명
            }

            val response = client.post("/api/wallet/bitcoin/transaction/complete") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            completedBtcTx = response.bodyAsText()
            assertTrue(completedBtcTx.isNotEmpty())
        }

        /*
         * 테스트 6: REST API를 통해 이더리움 MPC 트랜잭션의 두 번째 서명 추가 및 완성이 정상적으로 이루어지는지 검증
         * 1. 이전 테스트에서 부분 서명이 정상적으로 생성되었는지 확인
         * 2. API 응답 상태 코드가 200 OK인지
         * 3. 트랜잭션 해시가 반환되었는지
         * 4. 반환된 해시가 이더리움 트랜잭션 해시 형식(0x로 시작)인지
         */
        run {
            assertNotNull(partialEthSig, "부분 서명된 이더리움 트랜잭션이 먼저 생성되어야 합니다")

            val request = EthereumCompleteRequestDTO().apply {
                firstSignature = partialEthSig
                secondParticipantIndex = 1 // 두 번째 참여자
            }

            val response = client.post("/api/wallet/ethereum/transaction/complete") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            completedEthTxHash = response.bodyAsText()
            assertTrue(completedEthTxHash.startsWith("0x"))
        }

        /*
         * 1. 이전 테스트에서 지갑이 정상적으로 생성되었는지 확인
         * 2. API 응답 상태 코드가 200 OK인지
         * 3. 잔액 정보가 반환되었는지
         * 4. 잔액이 0 이상의 값인지
         */
        run {
            assertNotNull(ethereumWallet, "이더리움 지갑이 먼저 생성되어야 합니다")
            val address = ethereumWallet.address

            val response = client.get("/api/wallet/ethereum/balance/$address")

            assertEquals(HttpStatusCode.OK, response.status)
            val balance = response.bodyAsText().toBigDecimalOrNull()
            assertNotNull(balance)
            assertTrue(balance >= BigDecimal.ZERO)
        }
    }
}
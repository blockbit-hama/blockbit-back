package unit.controller.wallet

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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName

class WalletControllerTest {

    private lateinit var bitcoinMultiSigService: BitcoinMultiSigService
    private lateinit var ethereumMpcService: EthereumMpcService

    private lateinit var mockBitcoinWallet: MultisigWalletDTO
    private lateinit var mockEthereumWallet: MpcWalletDTO
    private lateinit var mockPartialBtcTx: PartiallySignedTransactionDTO
    private lateinit var mockPartialEthSig: PartialSignatureDTO

    @BeforeTest
    fun setUp() {
        // 서비스 모킹
        bitcoinMultiSigService = mockk()
        ethereumMpcService = mockk()

        // 비트코인 지갑 목업
        mockBitcoinWallet = MultisigWalletDTO().apply {
            address = "2N8hwP1WmJrFF5QWABn38y63uYLhnJYJYTF"
            redeemScript = "mock_redeem_script"
            publicKeys = listOf("pubkey1", "pubkey2", "pubkey3")
            privateKeys = listOf("privkey1", "privkey2", "privkey3")
        }

        // 이더리움 지갑 목업
        mockEthereumWallet = MpcWalletDTO().apply {
            walletId = "mock-wallet-id"
            address = "0x1234567890123456789012345678901234567890"
            publicKey = "mock_public_key"
        }

        // 부분 서명된 비트코인 트랜잭션 목업
        mockPartialBtcTx = PartiallySignedTransactionDTO().apply {
            transactionHex = "mock_tx_hex"
            signatureHex = "mock_sig_hex"
            publicKeyHex = "mock_pub_key_hex"
            redeemScriptHex = "mock_redeem_script_hex"
        }

        // 부분 서명된 이더리움 트랜잭션 목업
        mockPartialEthSig = PartialSignatureDTO().apply {
            walletId = "mock-wallet-id"
            transactionHash = "0xmock_tx_hash"
            partialSignature = "mock_partial_sig"
            participantIndex = 0
            rawTransaction = "0xmock_raw_tx"
        }
    }

    @Test
    @DisplayName("비트코인 지갑 생성 API 테스트")
    fun testCreateBitcoinWallet() = testApplication {
        // 모킹 설정
        every { bitcoinMultiSigService.createMultisigWallet() } returns mockBitcoinWallet

        // 앱 설정
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                walletRoutes(bitcoinMultiSigService, ethereumMpcService)
            }
        }

        // 테스트 요청 및 검증
        val response = client.post("/api/wallet/bitcoin/create")
        assertEquals(HttpStatusCode.Created, response.status)

        // 응답 내용 검증 (실제 구현에서는 deserialize하여 객체 비교 가능)
        val responseBody = response.bodyAsText()
        assert(responseBody.contains(mockBitcoinWallet.address))
    }

    @Test
    @DisplayName("비트코인 트랜잭션 생성 API 테스트")
    fun testCreateBitcoinTransaction() = testApplication {
        // 요청 데이터
        val request = BitcoinTransactionRequestDTO().apply {
            fromAddress = "2N8hwP1WmJrFF5QWABn38y63uYLhnJYJYTF"
            toAddress = "mwCwTceJvYV27KXBc3NJZys6CjsgsoeHmf"
            amountSatoshi = 10000L
            redeemScriptHex = "mock_redeem_script_hex"
            privateKeyHex = "mock_private_key_hex"
        }

        // 모킹 설정
        every {
            bitcoinMultiSigService.createMultisigTransaction(
                any(), any(), any(), any(), any()
            )
        } returns mockPartialBtcTx

        // 앱 설정
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                walletRoutes(bitcoinMultiSigService, ethereumMpcService)
            }
        }

        // 테스트 요청 및 검증
        val response = client.post("/api/wallet/bitcoin/transaction/create") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(BitcoinTransactionRequestDTO.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assert(responseBody.contains(mockPartialBtcTx.transactionHex))
    }

    @Test
    @DisplayName("이더리움 지갑 생성 API 테스트")
    fun testCreateEthereumWallet() = testApplication {
        // 모킹 설정
        every { ethereumMpcService.createMpcWallet() } returns mockEthereumWallet

        // 앱 설정
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                walletRoutes(bitcoinMultiSigService, ethereumMpcService)
            }
        }

        // 테스트 요청 및 검증
        val response = client.post("/api/wallet/ethereum/create")
        assertEquals(HttpStatusCode.Created, response.status)

        val responseBody = response.bodyAsText()
        assert(responseBody.contains(mockEthereumWallet.walletId))
        assert(responseBody.contains(mockEthereumWallet.address))
    }

    @Test
    @DisplayName("이더리움 트랜잭션 생성 API 테스트")
    fun testCreateEthereumTransaction() = testApplication {
        // 요청 데이터
        val request = EthereumTransactionRequestDTO().apply {
            walletId = "mock-wallet-id"
            participantIndex = 0
            toAddress = "0x9876543210987654321098765432109876543210"
            amount = "0.1" // String으로 변환
        }

        // 모킹 설정
        every {
            ethereumMpcService.createPartialSignature(
                any(), any(), any(), any()
            )
        } returns mockPartialEthSig

        // 앱 설정
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                walletRoutes(bitcoinMultiSigService, ethereumMpcService)
            }
        }

        // 테스트 요청 및 검증
        val response = client.post("/api/wallet/ethereum/transaction/create") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(EthereumTransactionRequestDTO.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assert(responseBody.contains(mockPartialEthSig.walletId))
        assert(responseBody.contains(mockPartialEthSig.transactionHash))
    }

    @Test
    @DisplayName("이더리움 잔액 조회 API 테스트")
    fun testGetEthereumBalance() = testApplication {
        // 테스트 데이터
        val address = "0x1234567890123456789012345678901234567890"
        val expectedBalance = BigDecimal.valueOf(1.5)

        // 모킹 설정
        every { ethereumMpcService.getBalance(address) } returns expectedBalance

        // 앱 설정
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                walletRoutes(bitcoinMultiSigService, ethereumMpcService)
            }
        }

        // 테스트 요청 및 검증
        val response = client.get("/api/wallet/ethereum/balance/$address")
        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = response.bodyAsText()
        assertEquals(expectedBalance.toString(), responseBody)
    }
}
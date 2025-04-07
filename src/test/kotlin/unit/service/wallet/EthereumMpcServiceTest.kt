package unit.service.wallet

import com.sg.service.wallet.EthereumMpcService
import kotlin.test.Test
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.EthGasPrice
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EthereumMpcServiceTest {

    @InjectMockKs
    private lateinit var ethereumMpcService: EthereumMpcService

    @MockK
    private lateinit var web3j: Web3j

    private lateinit var testWalletId: String
    private lateinit var testAddress: String

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        testWalletId = "test-wallet-id"
        testAddress = "0x1234567890123456789012345678901234567890"

        // GasPrice 모킹
        val ethGasPrice = mockk<EthGasPrice>()
        every { ethGasPrice.gasPrice } returns BigInteger.valueOf(20_000_000_000L) // 20 Gwei

        val gasPriceRequest = mockk<Request<*, EthGasPrice>>()
        every { gasPriceRequest.send() } returns ethGasPrice
        every { web3j.ethGasPrice() } returns gasPriceRequest

        // Balance 모킹
        val ethGetBalance = mockk<EthGetBalance>()
        every { ethGetBalance.balance } returns BigInteger.valueOf(1_000_000_000_000_000_000L) // 1 ETH

        val balanceRequest = mockk<Request<*, EthGetBalance>>()
        every { balanceRequest.send() } returns ethGetBalance
        every { web3j.ethGetBalance(any(), eq(DefaultBlockParameterName.LATEST)) } returns balanceRequest

        // TransactionCount 모킹 (getNonce 메소드에서 필요)
        val ethGetTransactionCount = mockk<EthGetTransactionCount>()
        every { ethGetTransactionCount.transactionCount } returns BigInteger.ZERO

        val transactionCountRequest = mockk<Request<*, EthGetTransactionCount>>()
        every { transactionCountRequest.send() } returns ethGetTransactionCount
        every { web3j.ethGetTransactionCount(any(), eq(DefaultBlockParameterName.LATEST)) } returns transactionCountRequest
    }

    @Test
    @DisplayName("이더리움 MPC 지갑이 올바르게 생성되는지 검증")
    /*
     * 1. 지갑 객체가 생성되었는지
     * 2. 지갑 ID가 생성되었는지
     * 3. 이더리움 주소가 생성되었는지
     * 4. 공개키가 생성되었는지
     * 5. 생성된 주소가 이더리움 주소 형식(0x로 시작, 42자)인지
     */
    fun testCreateMpcWallet() {
        // when
        val wallet = ethereumMpcService.createMpcWallet()

        // then
        assertNotNull(wallet)
        assertNotNull(wallet.walletId)
        assertNotNull(wallet.address)
        assertNotNull(wallet.publicKey)

        // 이더리움 주소 형식 테스트
        assertTrue(wallet.address.startsWith("0x"))
        assertEquals(42, wallet.address.length)
    }

    @Test
    @DisplayName("MPC 방식에서 첫 번째 참여자의 부분 서명이 올바르게 생성되는지 검증")
    /*
     * 1. 부분 서명 객체가 생성되었는지
     * 2. 트랜잭션 해시가 생성되었는지
     * 3. 부분 서명 데이터가 있는지
     * 4. 참여자 인덱스가 올바른지
     * 5. 원시 트랜잭션 데이터가 있는지
     */
    fun testCreatePartialSignature() {
        // given
        val toAddress = "0x9876543210987654321098765432109876543210"
        val amount = BigDecimal.valueOf(0.1) // 0.1 ETH
        val participantIndex = 0

        // MPC 지갑 생성 (테스트용 키 공유 생성을 위해)
        val wallet = ethereumMpcService.createMpcWallet()

        // when
        val partialSig = ethereumMpcService.createPartialSignature(
            wallet.walletId, participantIndex, toAddress, amount.toString())

        // then
        assertNotNull(partialSig)
        assertEquals(wallet.walletId, partialSig.walletId)
        assertNotNull(partialSig.transactionHash)
        assertNotNull(partialSig.partialSignature)
        assertEquals(participantIndex, partialSig.participantIndex)
        assertNotNull(partialSig.rawTransaction)
    }

    @Test
    @DisplayName("부분 서명들을 결합하여 완전한 트랜잭션을 생성하고 제출하는 기능 검증")
    /*
     * 1. 트랜잭션 해시가 생성되었는지
     * 2. 생성된 해시가 이더리움 트랜잭션 해시 형식(0x로 시작)인지
     * 3. MPC 서명 결합이 정상적으로 이루어졌는지
     */
    fun testCompleteAndSubmitTransaction() {
        // given
        val toAddress = "0x9876543210987654321098765432109876543210"
        val amount = BigDecimal.valueOf(0.1) // 0.1 ETH

        // MPC 지갑 생성 (테스트용 키 공유 생성을 위해)
        val wallet = ethereumMpcService.createMpcWallet()

        // 첫 번째 부분 서명 생성
        val firstSig = ethereumMpcService.createPartialSignature(
            wallet.walletId, 0, toAddress, amount.toString())

        // when
        val txHash = ethereumMpcService.completeAndSubmitTransaction(firstSig, 1)

        // then
        assertNotNull(txHash)
        assertTrue(txHash.startsWith("0x"))
    }

    @Test
    @DisplayName("이더리움 주소의 잔액을 올바르게 조회하는지 검증")
    /*
     * 1. 잔액이 null이 아닌지
     * 2. 모킹된 응답 값(1 ETH)과 일치하는지
     * 3. 네트워크 요청이 올바른 매개변수로 이루어졌는지
     */
    fun testGetBalance() {
        // given
        val address = "0x1234567890123456789012345678901234567890"

        // when
        val balance = ethereumMpcService.getBalance(address)

        // then
        assertNotNull(balance)
        assertEquals(1.0, balance.toDouble())

        // verify 메서드로 web3j.ethGetBalance가 올바른 매개변수로 호출되었는지 검증
        verify { web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST) }
    }
}
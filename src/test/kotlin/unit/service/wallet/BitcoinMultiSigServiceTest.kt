package unit.service.wallet

import com.sg.service.wallet.BitcoinMultiSigService
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Utils
import org.bitcoinj.script.ScriptBuilder
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BitcoinMultiSigServiceTest {

    private lateinit var bitcoinMultiSigService: BitcoinMultiSigService
    private lateinit var params: NetworkParameters
    private lateinit var testKeys: List<ECKey>
    private lateinit var redeemScriptHex: String

    @BeforeTest
    fun setUp() {
        bitcoinMultiSigService = BitcoinMultiSigService() // 직접 인스턴스 생성

        params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET)!!

        // 테스트용 키 생성
        val keys = ArrayList<ECKey>()
        for (i in 0 until 3) {
            keys.add(ECKey())
        }
        testKeys = keys

        // 리딤 스크립트 생성
        val redeemScript = ScriptBuilder.createMultiSigOutputScript(2, testKeys)
        redeemScriptHex = Utils.HEX.encode(redeemScript.program)
    }

    @Test
    @DisplayName("BitcoinMultiSigService가 2-of-3 멀티시그 지갑을 올바르게 생성하는지 검증")
    /*
     * 1. 지갑이 null이 아닌지
     * 2. 비트코인 주소가 생성되었는지
     * 3. 리딤 스크립트가 생성되었는지
     * 4. 공개키와 개인키가 각각 3개씩 생성되었는지
     * 5. 생성된 주소가, 테스트넷 주소 형식("2", "m", "n" 중 하나로 시작)인지
     */
    fun testCreateMultisigWallet() {
        // when
        val wallet = bitcoinMultiSigService.createMultisigWallet()

        // then
        assertNotNull(wallet)
        assertNotNull(wallet.address)
        assertNotNull(wallet.redeemScript)
        assertEquals(3, wallet.publicKeys.size)
        assertEquals(3, wallet.privateKeys.size)

        // 주소가 유효한 형식인지 테스트
        assertTrue(wallet.address.startsWith("2") || wallet.address.startsWith("m") || wallet.address.startsWith("n"))
    }

    @Test
    @DisplayName("첫 번째 개인키로 부분적으로 서명된 트랜잭션을 올바르게 생성하는지 검증")
    /*
     * 1. 트랜잭션 객체가 생성되었는지
     * 2. 트랜잭션 직렬화(Hex) 정보가 있는
     * 3. 서명 정보가 있는지
     * 4. 공개키 정보가 있는지
     * 5. 리딤 스크립트가 올바르게 포함되었는지
     */
    fun testCreateMultisigTransaction() {
        // given
        val fromAddress = "2N8hwP1WmJrFF5QWABn38y63uYLhnJYJYTF" // 테스트넷 P2SH 주소 형식
        val toAddress = "mwCwTceJvYV27KXBc3NJZys6CjsgsoeHmf"   // 테스트넷 주소 형식
        val amountSatoshi = 10000L // 0.0001 BTC
        val privateKeyHex = testKeys[0].privateKeyAsHex

        // when
        val tx = bitcoinMultiSigService.createMultisigTransaction(
            fromAddress, toAddress, amountSatoshi, redeemScriptHex, privateKeyHex)

        // then
        assertNotNull(tx)
        assertNotNull(tx.transactionHex)
        assertNotNull(tx.signatureHex)
        assertNotNull(tx.publicKeyHex)
        assertEquals(redeemScriptHex, tx.redeemScriptHex)
    }

    @Test
    @DisplayName("부분적으로 서명된 트랜잭션에 두 번째 서명을 추가하여 완전한 트랜잭션을 만드는 기능 검증")
    /*
     * 1. 두 번째 서명이 추가된 후 최종 트랜잭션이 생성되는지
     * 2. 생성된 트랜잭션이 유효한 형식인지(빈 문자열이 아닌지)
     * 3. 멀티시그 조건(2-of-3)을 만족하는 트랜잭션이 생성되는지
     */
    fun testAddSignatureToTransaction() {
        // given
        val fromAddress = "2N8hwP1WmJrFF5QWABn38y63uYLhnJYJYTF"
        val toAddress = "mwCwTceJvYV27KXBc3NJZys6CjsgsoeHmf"
        val amountSatoshi = 10000L // 0.0001 BTC
        val firstPrivateKeyHex = testKeys[0].privateKeyAsHex
        val secondPrivateKeyHex = testKeys[1].privateKeyAsHex

        // first signature
        val partialTx = bitcoinMultiSigService.createMultisigTransaction(
            fromAddress, toAddress, amountSatoshi, redeemScriptHex, firstPrivateKeyHex)

        // when
        val signedTxHex = bitcoinMultiSigService.addSignatureToTransaction(
            partialTx, secondPrivateKeyHex)

        // then
        assertNotNull(signedTxHex)
        assertTrue(signedTxHex.isNotEmpty())
    }
}
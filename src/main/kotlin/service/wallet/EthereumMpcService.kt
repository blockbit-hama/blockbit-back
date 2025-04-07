package com.sg.service.wallet

import com.google.common.primitives.Bytes

import com.sg.dto.wallet.MpcWalletDTO
import com.sg.dto.wallet.PartialSignatureDTO
import org.slf4j.LoggerFactory
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EthereumMpcService {

    private val logger = LoggerFactory.getLogger(EthereumMpcService::class.java)
    private val web3j: Web3j
    private val infuraUrl = "https://goerli.infura.io/v3/YOUR_INFURA_KEY" // 테스트넷 사용

    // 키 공유 저장소 (실제로는 보다 안전한 저장소 사용)
    private val keyShares = ConcurrentHashMap<String, KeyShare>()

    init {
        this.web3j = Web3j.build(HttpService(infuraUrl))
    }

    /**
     * MPC를 통한 이더리움 지갑 생성
     * 이 예제에서는 2-of-3 MPC 구현 시뮬레이션
     */
    fun createMpcWallet(): MpcWalletDTO {
        try {
            // MPC 키 생성 세션 ID
            val sessionId = UUID.randomUUID().toString()

            // 각 참여자의 키 공유 생성 (실제 MPC에서는 참여자 간 안전한 통신 필요)
            val shares = Array<KeyShare>(3) { KeyShare() }

            // 실제 MPC 대신 데모 목적으로 ECKeyPair 생성
            val baseKeyPair = Keys.createEcKeyPair()
            val privateKeyHex = baseKeyPair.privateKey.toString(16)
            val publicKeyHex = baseKeyPair.publicKey.toString(16)

            // 시뮬레이션을 위한 키 분할 (실제로는 MPC 프로토콜 구현)
            for (i in 0 until 3) {
                val share = KeyShare()
                share.id = sessionId
                share.index = i
                share.privateKeyShare = "${privateKeyHex}_share_$i" // 실제로는 안전한 비밀 분산 방식 사용

                shares[i] = share
                keyShares["${sessionId}_$i"] = share
            }

            // 이더리움 주소 계산
            val address = computeAddressFromPublicKey(publicKeyHex)

            // DB에 지갑 정보 저장 (실제 구현에서 추가)

            // 결과 생성
            return MpcWalletDTO().apply {
                walletId = sessionId
                this.address = address
                publicKey = publicKeyHex
            }
        } catch (e: Exception) {
            logger.error("MPC 지갑 생성 오류", e)
            throw RuntimeException("MPC 지갑 생성 실패", e)
        }
    }

    /**
     * MPC를 통한 트랜잭션 서명
     * 이 예제에서는 2-of-3 MPC 구현의 첫 번째 서명 단계 시뮬레이션
     */
    fun createPartialSignature(
        walletId: String,
        participantIndex: Int,
        toAddress: String,
        etherAmount: String
    ): PartialSignatureDTO {

        try {
            // 키 공유 가져오기
            val keyShare = keyShares["${walletId}_$participantIndex"]
                ?: throw RuntimeException("키 공유를 찾을 수 없습니다")

            // 지갑 정보 가져오기 (실제로는 DB에서 조회)
            val walletAddress = ""  // 실제 구현에서는 DB 또는 저장소에서 조회

            // 트랜잭션 데이터 준비
            val gasPrice = web3j.ethGasPrice().send().gasPrice
            val gasLimit = BigInteger.valueOf(21000) // 기본 전송

            val nonce = getNonce(walletAddress)
            val value = Convert.toWei(etherAmount, Convert.Unit.ETHER).toBigInteger()

            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, toAddress, value)

            val encodedTransaction = TransactionEncoder.encode(rawTransaction)
            val transactionHash = Numeric.toHexString(Hash.sha3(encodedTransaction))

            // 부분 서명 생성 시뮬레이션 (실제로는 MPC 프로토콜 구현)
            val partialSignature = "partial_signature_${keyShare.privateKeyShare}_$transactionHash"

            // 결과 생성
            return PartialSignatureDTO().apply {
                this.walletId = walletId
                this.transactionHash = transactionHash
                this.partialSignature = partialSignature
                this.participantIndex = participantIndex
                this.rawTransaction = Numeric.toHexString(encodedTransaction)
            }
        } catch (e: Exception) {
            logger.error("부분 서명 생성 오류", e)
            throw RuntimeException("부분 서명 생성 실패", e)
        }
    }

    /**
     * MPC 트랜잭션 완료 (두 번째 서명 추가) 시뮬레이션
     */
    fun completeAndSubmitTransaction(
        firstSignature: PartialSignatureDTO,
        secondParticipantIndex: Int
    ): String {

        try {
            val walletId = firstSignature.walletId

            // 두 번째 키 공유 가져오기
            val secondKeyShare = keyShares["${walletId}_$secondParticipantIndex"]
                ?: throw RuntimeException("두 번째 키 공유를 찾을 수 없습니다")

            // 두 번째 부분 서명 생성 시뮬레이션
            val secondPartialSignature = "partial_signature_${secondKeyShare.privateKeyShare}_${firstSignature.transactionHash}"

            // 서명 결합 시뮬레이션 (실제로는 MPC 프로토콜에 따른 서명 결합)
            val partialSignatures = arrayOf(
                firstSignature.partialSignature,
                secondPartialSignature
            )

            // 데모 목적으로 임의의 서명 생성 (실제로는 부분 서명을 결합)
            val r = ByteArray(32)
            val s = ByteArray(32)
            val v: Byte = 0x1b
            SecureRandom().nextBytes(r)
            SecureRandom().nextBytes(s)
            val signature = ByteArray(65)
            System.arraycopy(r, 0, signature, 0, 32)
            System.arraycopy(s, 0, signature, 32, 32)
            signature[64] = v

            // 서명된 트랜잭션 생성
            val rawTransactionBytes = Numeric.hexStringToByteArray(firstSignature.rawTransaction)
            val signedTransaction = Bytes.concat(rawTransactionBytes, signature)

            // 트랜잭션 전송 (실제로는 서명된 트랜잭션 전송)
            // 데모 목적으로 전송 생략, 임의의 트랜잭션 해시 반환
            return "0x" + Numeric.toHexString(Hash.sha3(signedTransaction)).substring(2)
        } catch (e: Exception) {
            logger.error("트랜잭션 완료 오류", e)
            throw RuntimeException("트랜잭션 완료 실패", e)
        }
    }

    /**
     * 지갑 잔액 조회
     */
    fun getBalance(address: String): BigDecimal {
        try {
            val ethGetBalance = web3j
                .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send()

            val wei = ethGetBalance.balance
            return Convert.fromWei(BigDecimal(wei), Convert.Unit.ETHER)
        } catch (e: Exception) {
            logger.error("잔액 조회 오류", e)
            throw RuntimeException("잔액 조회 실패", e)
        }
    }

    /**
     * 공개키로부터 이더리움 주소 계산
     */
    private fun computeAddressFromPublicKey(publicKey: String): String {
        val cleanPublicKey = if (publicKey.startsWith("0x")) publicKey.substring(2) else publicKey
        val publicKeyBytes = Numeric.hexStringToByteArray(cleanPublicKey)
        val addressBytes = Hash.sha3(publicKeyBytes)
        return "0x" + Numeric.toHexString(addressBytes).substring(24)
    }

    /**
     * 계정의 현재 nonce 조회
     */
    @Throws(IOException::class)
    private fun getNonce(address: String): BigInteger {
        val ethGetTransactionCount = web3j
            .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
            .send()
        return ethGetTransactionCount.transactionCount
    }

    // MPC 키 공유 클래스 (실제 구현에서는 MPC 라이브러리의 클래스 사용)
    private class KeyShare {
        var id: String = ""
        var index: Int = 0
        var privateKeyShare: String = ""
    }
}
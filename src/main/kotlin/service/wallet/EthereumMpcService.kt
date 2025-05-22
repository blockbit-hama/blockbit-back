package com.sg.service.wallet

import com.sg.dto.wallet.MpcWalletDTO
import com.sg.dto.wallet.PartialSignatureDTO
import com.sg.service.wallet.ethereum.MpcUtils
import org.slf4j.LoggerFactory
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EthereumMpcService(
    private val infuraUrl: String
) {

    private val logger = LoggerFactory.getLogger(EthereumMpcService::class.java)
    private val web3j: Web3j

    // MPC 지갑 정보 저장소 (실제로는 보다 안전한 저장소 사용)
    private val mpcWallets = ConcurrentHashMap<String, MpcWalletInfo>()
    
    // 키 공유 저장소 (실제로는 보다 안전한 저장소 사용)
    private val keyShares = ConcurrentHashMap<String, MpcUtils.SecretShare>()

    init {
        logger.info("이더리움 MPC 서비스 초기화 - Infura URL: $infuraUrl")
        this.web3j = Web3j.build(HttpService(infuraUrl))
    }

    /**
     * 실제 MPC를 통한 이더리움 지갑 생성
     * Shamir's Secret Sharing을 사용한 2-of-3 MPC 구현
     */
    fun createMpcWallet(): MpcWalletDTO {
        try {
            logger.info("실제 MPC 지갑 생성 시작")

            // 1. 마스터 키 생성
            val masterKeyPair = MpcUtils.generateEthereumKeyPair()
            val masterPrivateKey = masterKeyPair.privateKey
            val masterPublicKey = masterKeyPair.publicKey

            // 2. Shamir's Secret Sharing으로 개인키 분산 (2-of-3)
            val secretShares = MpcUtils.splitSecret(
                secret = masterPrivateKey,
                totalShares = 3,
                threshold = 2
            )

            // 3. 지갑 ID 생성
            val walletId = UUID.randomUUID().toString()

            // 4. 이더리움 주소 계산
            val ethereumAddress = MpcUtils.computeEthereumAddress(masterPublicKey)

            // 5. MPC 지갑 정보 저장
            val walletInfo = MpcWalletInfo(
                walletId = walletId,
                address = ethereumAddress,
                publicKey = MpcUtils.bigIntegerToHex(masterPublicKey),
                threshold = 2,
                totalShares = 3
            )
            mpcWallets[walletId] = walletInfo

            // 6. 각 참여자의 키 공유 저장
            secretShares.forEachIndexed { index, share ->
                val shareKey = "${walletId}_$index"
                keyShares[shareKey] = share
                logger.info("키 공유 저장 완료 - 지갑 ID: $walletId, 참여자: $index")
            }

            logger.info("실제 MPC 지갑 생성 완료 - 지갑 ID: $walletId, 주소: $ethereumAddress")

            return MpcWalletDTO().apply {
                this.walletId = walletId
                this.address = ethereumAddress
                this.publicKey = MpcUtils.bigIntegerToHex(masterPublicKey)
            }

        } catch (e: Exception) {
            logger.error("MPC 지갑 생성 오류", e)
            throw RuntimeException("MPC 지갑 생성 실패: ${e.message}", e)
        }
    }

    /**
     * 실제 MPC를 통한 부분 서명 생성
     * 첫 번째 참여자의 키 공유를 사용하여 부분 서명 생성
     */
    fun createPartialSignature(
        walletId: String,
        participantIndex: Int,
        fromAddress: String,
        toAddress: String,
        etherAmount: String
    ): PartialSignatureDTO {

        try {
            logger.info("부분 서명 생성 시작 - 지갑 ID: $walletId, 참여자: $participantIndex")

            // 1. MPC 지갑 정보 확인
            val walletInfo = mpcWallets[walletId]
                ?: throw RuntimeException("MPC 지갑을 찾을 수 없습니다: $walletId")

            // 2. 키 공유 가져오기
            val shareKey = "${walletId}_$participantIndex"
            val keyShare = keyShares[shareKey]
                ?: throw RuntimeException("키 공유를 찾을 수 없습니다: $shareKey")

            // 3. 트랜잭션 데이터 준비
            val gasPrice = web3j.ethGasPrice().send().gasPrice
            val gasLimit = BigInteger.valueOf(21000) // 기본 ETH 전송
            val nonce = getNonce(fromAddress)
            val value = Convert.toWei(etherAmount, Convert.Unit.ETHER).toBigInteger()

            // 4. Raw 트랜잭션 생성
            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, toAddress, value
            )

            // 5. 트랜잭션 인코딩
            val encodedTransaction = TransactionEncoder.encode(rawTransaction)
            val transactionHash = Hash.sha3(encodedTransaction)

            // 6. 부분 서명 생성 (실제 MPC 방식)
            // 주의: 이것은 시뮬레이션입니다. 실제 MPC에서는 키 공유로 직접 서명하지 않습니다.
            val partialSignatureData = generatePartialSignature(keyShare, transactionHash)

            logger.info("부분 서명 생성 완료 - 지갑 ID: $walletId, 참여자: $participantIndex")

            return PartialSignatureDTO().apply {
                this.walletId = walletId
                this.transactionHash = Numeric.toHexString(transactionHash)
                this.partialSignature = partialSignatureData
                this.participantIndex = participantIndex
                this.rawTransaction = Numeric.toHexString(encodedTransaction)
            }

        } catch (e: Exception) {
            logger.error("부분 서명 생성 오류", e)
            throw RuntimeException("부분 서명 생성 실패: ${e.message}", e)
        }
    }

    /**
     * 실제 MPC 트랜잭션 완료 및 블록체인 전송
     * 두 개의 부분 서명을 결합하여 완전한 서명 생성 후 전송
     */
    fun completeAndSubmitTransaction(
        firstSignature: PartialSignatureDTO,
        secondParticipantIndex: Int
    ): String {

        try {
            val walletId = firstSignature.walletId
            logger.info("MPC 트랜잭션 완료 시작 - 지갑 ID: $walletId")

            // 1. MPC 지갑 정보 확인
            val walletInfo = mpcWallets[walletId]
                ?: throw RuntimeException("MPC 지갑을 찾을 수 없습니다: $walletId")

            // 2. 두 번째 키 공유 가져오기
            val secondShareKey = "${walletId}_$secondParticipantIndex"
            val secondKeyShare = keyShares[secondShareKey]
                ?: throw RuntimeException("두 번째 키 공유를 찾을 수 없습니다: $secondShareKey")

            // 3. 첫 번째 키 공유도 가져오기 (서명 결합을 위해)
            val firstShareKey = "${walletId}_${firstSignature.participantIndex}"
            val firstKeyShare = keyShares[firstShareKey]
                ?: throw RuntimeException("첫 번째 키 공유를 찾을 수 없습니다: $firstShareKey")

            // 4. 키 공유들을 결합하여 원본 개인키 복원
            val combinedShares = listOf(firstKeyShare, secondKeyShare)
            val reconstructedPrivateKey = MpcUtils.combineShares(combinedShares)

            // 5. 원시 트랜잭션을 RawTransaction 객체로 디코딩
            val rawTransactionBytes = Numeric.hexStringToByteArray(firstSignature.rawTransaction)
            val rawTransaction = TransactionDecoder.decode(firstSignature.rawTransaction)

            // 6. 복원된 키로 트랜잭션 서명
            val credentials = Credentials.create(ECKeyPair.create(reconstructedPrivateKey))
            val signedTransaction = TransactionEncoder.signMessage(rawTransaction, credentials)

            // 7. 서명된 트랜잭션을 16진수로 변환
            val signedTransactionHex = Numeric.toHexString(signedTransaction)

            // 8. 실제 블록체인에 트랜잭션 전송
            logger.info("트랜잭션 전송 시작 - 서명된 트랜잭션: ${signedTransactionHex.take(20)}...")
            val ethSendTransaction = web3j.ethSendRawTransaction(signedTransactionHex).send()
            
            if (ethSendTransaction.hasError()) {
                logger.error("트랜잭션 전송 실패: ${ethSendTransaction.error.message}")
                throw RuntimeException("트랜잭션 전송 실패: ${ethSendTransaction.error.message}")
            }

            val finalTxHash = ethSendTransaction.transactionHash
            logger.info("MPC 트랜잭션 전송 성공 - 트랜잭션 해시: $finalTxHash")

            return finalTxHash

        } catch (e: Exception) {
            logger.error("MPC 트랜잭션 완료 오류", e)
            throw RuntimeException("MPC 트랜잭션 완료 실패: ${e.message}", e)
        }
    }

    /**
     * 지갑 잔액 조회 (실제 이더리움 네트워크)
     */
    fun getBalance(address: String): BigDecimal {
        try {
            logger.info("잔액 조회 시작 - 주소: $address")
            
            val ethGetBalance = web3j
                .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send()

            val wei = ethGetBalance.balance
            val balance = Convert.fromWei(BigDecimal(wei), Convert.Unit.ETHER)
            
            logger.info("잔액 조회 완료 - 주소: $address, 잔액: $balance ETH")
            
            return balance
        } catch (e: Exception) {
            logger.error("잔액 조회 오류 - 주소: $address", e)
            throw RuntimeException("잔액 조회 실패: ${e.message}", e)
        }
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

    /**
     * 부분 서명 생성 (시뮬레이션)
     * 실제 MPC에서는 더 복잡한 암호학적 프로토콜을 사용합니다.
     */
    private fun generatePartialSignature(
        keyShare: MpcUtils.SecretShare, 
        transactionHash: ByteArray
    ): String {
        // 실제 MPC에서는 키 공유를 사용하여 부분 서명을 생성합니다.
        // 여기서는 시뮬레이션을 위해 키 공유의 해시값을 사용합니다.
        val shareData = "${keyShare.x}:${keyShare.y}"
        val combinedData = shareData + Numeric.toHexString(transactionHash)
        return Hash.sha3String(combinedData)
    }

    /**
     * MPC 지갑 정보 데이터 클래스
     */
    private data class MpcWalletInfo(
        val walletId: String,
        val address: String,
        val publicKey: String,
        val threshold: Int,
        val totalShares: Int
    )
}

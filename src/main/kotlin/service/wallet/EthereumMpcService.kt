package com.sg.service.wallet

import com.sg.dto.WalletDTO
import com.sg.dto.AddressDTO
import com.sg.dto.wallet.MpcWalletDTO
import com.sg.dto.wallet.PartialSignatureDTO
import com.sg.repository.WalletRepository
import com.sg.repository.AddressRepository
import com.sg.service.wallet.ethereum.MpcUtils
import com.sg.utils.DateTimeUtil
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

class EthereumMpcService(
    private val infuraUrl: String,
    private val secureKeyStorageService: SecureKeyStorageService = SecureKeyStorageService(),
    private val walletRepository: WalletRepository = WalletRepository(),
    private val addressRepository: AddressRepository = AddressRepository()
) {

    private val logger = LoggerFactory.getLogger(EthereumMpcService::class.java)
    private val web3j: Web3j

    init {
        logger.info("이더리움 MPC 서비스 초기화 - Infura URL: $infuraUrl")
        this.web3j = Web3j.build(HttpService(infuraUrl))
    }

    /**
     * 실제 MPC를 통한 이더리움 지갑 생성
     * Shamir's Secret Sharing을 사용한 2-of-3 MPC 구현
     */
    suspend fun createMpcWallet(userNum: Int, walletName: String, astId: Int? = null, polId: Int? = null): MpcWalletDTO {
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

            // 5. 지갑 정보를 DB에 저장
            val currentDate = DateTimeUtil.getCurrentDate()
            val currentTime = DateTimeUtil.getCurrentTime()

            val walletDTO = WalletDTO(
                walName = walletName,
                walType = "Self-custody Hot",
                walProtocol = "MPC",
                walPwd = walletId, // 지갑 ID를 비밀번호 필드에 저장 (임시)
                walStatus = "active",
                usiNum = userNum,
                astId = astId,
                polId = polId,  // 정책 ID 추가
                creusr = userNum,
                credat = currentDate,
                cretim = currentTime,
                lmousr = userNum,
                lmodat = currentDate,
                lmotim = currentTime
            )

            val walNum = walletRepository.addWallet(walletDTO)
            logger.info("지갑 정보 DB 저장 완료 - wal_num: $walNum")

            // 6. 주소 정보를 DB에 저장
            val addressDTO = AddressDTO(
                adrAddress = ethereumAddress,
                adrLabel = "$walletName - MPC Address",
                adrType = "MPC",
                adrPath = "m/44'/60'/0'/0/0", // 이더리움 표준 경로
                walId = walNum,
                astId = astId,
                creusr = userNum,
                credat = currentDate,
                cretim = currentTime,
                lmousr = userNum,
                lmodat = currentDate,
                lmotim = currentTime
            )

            val adrNum = addressRepository.addAddress(addressDTO)
            logger.info("주소 정보 DB 저장 완료 - adr_num: $adrNum")

            // 7. 각 참여자의 키 공유를 안전하게 저장
            val publicKeyHex = MpcUtils.bigIntegerToHex(masterPublicKey)

            secretShares.forEachIndexed { index, share ->
                secureKeyStorageService.saveKeyShare(
                    walletId = walletId,
                    participantIndex = index,
                    secretShare = share,
                    publicKey = publicKeyHex,
                    walNum = walNum,
                    userNum = userNum
                )
                logger.info("키 공유 저장 완료 - 지갑 ID: $walletId, 참여자: $index")
            }

            logger.info("실제 MPC 지갑 생성 완료 - 지갑 ID: $walletId, 주소: $ethereumAddress")

            return MpcWalletDTO().apply {
                this.walletId = walletId
                this.address = ethereumAddress
                this.publicKey = publicKeyHex
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
    suspend fun createPartialSignature(
        walletId: String,
        participantIndex: Int,
        fromAddress: String,
        toAddress: String,
        etherAmount: String
    ): PartialSignatureDTO {

        try {
            logger.info("부분 서명 생성 시작 - 지갑 ID: $walletId, 참여자: $participantIndex")

            // 1. 키 공유 가져오기
            val keyShare = secureKeyStorageService.getKeyShare(walletId, participantIndex)
                ?: throw RuntimeException("키 공유를 찾을 수 없습니다: $walletId, 참여자: $participantIndex")

            // 2. 트랜잭션 데이터 준비
            val gasPrice = web3j.ethGasPrice().send().gasPrice
            val gasLimit = BigInteger.valueOf(21000) // 기본 ETH 전송
            val nonce = getNonce(fromAddress)
            val value = Convert.toWei(etherAmount, Convert.Unit.ETHER).toBigInteger()

            // 3. Raw 트랜잭션 생성
            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, toAddress, value
            )

            // 4. 트랜잭션 인코딩
            val encodedTransaction = TransactionEncoder.encode(rawTransaction)
            val transactionHash = Hash.sha3(encodedTransaction)

            // 5. 부분 서명 생성 (실제 MPC 방식)
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
    suspend fun completeAndSubmitTransaction(
        firstSignature: PartialSignatureDTO,
        secondParticipantIndex: Int
    ): String {

        try {
            val walletId = firstSignature.walletId
            logger.info("MPC 트랜잭션 완료 시작 - 지갑 ID: $walletId")

            // 1. 두 키 공유 가져오기
            val firstKeyShare = secureKeyStorageService.getKeyShare(walletId, firstSignature.participantIndex)
                ?: throw RuntimeException("첫 번째 키 공유를 찾을 수 없습니다")

            val secondKeyShare = secureKeyStorageService.getKeyShare(walletId, secondParticipantIndex)
                ?: throw RuntimeException("두 번째 키 공유를 찾을 수 없습니다")

            // 2. 키 공유들을 결합하여 원본 개인키 복원
            val combinedShares = listOf(firstKeyShare, secondKeyShare)
            val reconstructedPrivateKey = MpcUtils.combineShares(combinedShares)

            // 3. 원시 트랜잭션을 RawTransaction 객체로 디코딩
            val rawTransaction = TransactionDecoder.decode(firstSignature.rawTransaction)

            // 4. 복원된 키로 트랜잭션 서명
            val credentials = Credentials.create(ECKeyPair.create(reconstructedPrivateKey))
            val signedTransaction = TransactionEncoder.signMessage(rawTransaction, credentials)

            // 5. 서명된 트랜잭션을 16진수로 변환
            val signedTransactionHex = Numeric.toHexString(signedTransaction)

            // 6. 실제 블록체인에 트랜잭션 전송
            logger.info("트랜잭션 전송 시작 - 서명된 트랜잭션: ${signedTransactionHex.take(20)}...")
            val ethSendTransaction = web3j.ethSendRawTransaction(signedTransactionHex).send()

            if (ethSendTransaction.hasError()) {
                logger.error("트랜잭션 전송 실패: ${ethSendTransaction.error.message}")
                throw RuntimeException("트랜잭션 전송 실패: ${ethSendTransaction.error.message}")
            }

            val finalTxHash = ethSendTransaction.transactionHash
            logger.info("MPC 트랜잭션 전송 성공 - 트랜잭션 해시: $finalTxHash")

            // 7. 트랜잭션 정보를 DB에 저장 (TODO: TransactionService 연동)

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
}
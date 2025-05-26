package com.sg.service.wallet

import com.sg.dto.wallet.KeyShareDTO
import com.sg.repository.KeyShareRepository
import com.sg.service.wallet.ethereum.MpcUtils
import com.sg.utils.DateTimeUtil
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class SecureKeyStorageService(
    private val keyShareRepository: KeyShareRepository = KeyShareRepository()
) {
    private val logger = LoggerFactory.getLogger(SecureKeyStorageService::class.java)

    // 암호화 관련 상수
    private val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val KEY_ALGORITHM = "AES"
    private val SECRET_KEY_FACTORY = "PBKDF2WithHmacSHA256"
    private val ITERATIONS = 65536
    private val KEY_LENGTH = 256

    // 마스터 키 (실제 환경에서는 환경 변수나 HSM에서 가져와야 함)
    private val MASTER_PASSWORD = System.getenv("MPC_MASTER_KEY") ?: "temporary-master-key-change-in-production"
    private val SALT = System.getenv("MPC_SALT")?.toByteArray() ?: "temporary-salt-change-in-production".toByteArray()

    /**
     * 키 공유 정보를 암호화하여 저장
     */
    suspend fun saveKeyShare(
        walletId: String,
        participantIndex: Int,
        secretShare: MpcUtils.SecretShare,
        publicKey: String? = null,
        walNum: Int? = null,
        userNum: Int? = null
    ): Int {
        try {
            logger.info("키 공유 저장 시작 - 지갑 ID: $walletId, 참여자: $participantIndex")

            // Y 값을 암호화
            val encryptedY = encryptData(secretShare.y.toString())

            // DTO 생성
            val keyShareDTO = KeyShareDTO(
                walletId = walletId,
                participantIndex = participantIndex,
                shareX = secretShare.x,
                shareY = encryptedY,
                publicKey = publicKey,
                walNum = walNum,
                creusr = userNum,
                credat = DateTimeUtil.getCurrentDate(),
                cretim = DateTimeUtil.getCurrentTime(),
                lmousr = userNum,
                lmodat = DateTimeUtil.getCurrentDate(),
                lmotim = DateTimeUtil.getCurrentTime()
            )

            // DB에 저장
            val kshNum = keyShareRepository.saveKeyShare(keyShareDTO)
            logger.info("키 공유 저장 완료 - ksh_num: $kshNum")

            return kshNum
        } catch (e: Exception) {
            logger.error("키 공유 저장 실패", e)
            throw RuntimeException("키 공유 저장 실패: ${e.message}", e)
        }
    }

    /**
     * 키 공유 정보를 조회하여 복호화
     */
    suspend fun getKeyShare(walletId: String, participantIndex: Int): MpcUtils.SecretShare? {
        try {
            logger.info("키 공유 조회 시작 - 지갑 ID: $walletId, 참여자: $participantIndex")

            val keyShareDTO = keyShareRepository.getKeyShare(walletId, participantIndex)
                ?: return null

            // Y 값을 복호화
            val decryptedY = decryptData(keyShareDTO.shareY)
            val yBigInteger = decryptedY.toBigInteger()

            logger.info("키 공유 조회 완료")
            return MpcUtils.SecretShare(keyShareDTO.shareX, yBigInteger)
        } catch (e: Exception) {
            logger.error("키 공유 조회 실패", e)
            throw RuntimeException("키 공유 조회 실패: ${e.message}", e)
        }
    }

    /**
     * 지갑의 모든 키 공유 정보 조회
     */
    suspend fun getAllKeyShares(walletId: String): List<MpcUtils.SecretShare> {
        try {
            logger.info("모든 키 공유 조회 시작 - 지갑 ID: $walletId")

            val keyShareDTOs = keyShareRepository.getKeySharesByWalletId(walletId)

            val secretShares = keyShareDTOs.map { dto ->
                val decryptedY = decryptData(dto.shareY)
                MpcUtils.SecretShare(dto.shareX, decryptedY.toBigInteger())
            }

            logger.info("모든 키 공유 조회 완료 - 개수: ${secretShares.size}")
            return secretShares
        } catch (e: Exception) {
            logger.error("모든 키 공유 조회 실패", e)
            throw RuntimeException("모든 키 공유 조회 실패: ${e.message}", e)
        }
    }

    /**
     * 키 공유 삭제
     */
    suspend fun deleteKeyShare(walletId: String, participantIndex: Int): Boolean {
        try {
            logger.info("키 공유 삭제 시작 - 지갑 ID: $walletId, 참여자: $participantIndex")
            val result = keyShareRepository.deleteKeyShare(walletId, participantIndex)
            logger.info("키 공유 삭제 완료")
            return result
        } catch (e: Exception) {
            logger.error("키 공유 삭제 실패", e)
            throw RuntimeException("키 공유 삭제 실패: ${e.message}", e)
        }
    }

    /**
     * 데이터 암호화
     */
    private fun encryptData(data: String): String {
        val keySpec = generateKeySpec()
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(data.toByteArray())

        // IV와 암호화된 데이터를 함께 저장
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * 데이터 복호화
     */
    private fun decryptData(encryptedData: String): String {
        val combined = Base64.getDecoder().decode(encryptedData)
        val iv = combined.sliceArray(0..15)
        val encrypted = combined.sliceArray(16 until combined.size)

        val keySpec = generateKeySpec()
        val cipher = Cipher.getInstance(ALGORITHM)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(encrypted)

        return String(decrypted)
    }

    /**
     * 암호화 키 생성
     */
    private fun generateKeySpec(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY)
        val spec = PBEKeySpec(MASTER_PASSWORD.toCharArray(), SALT, ITERATIONS, KEY_LENGTH)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, KEY_ALGORITHM)
    }
}
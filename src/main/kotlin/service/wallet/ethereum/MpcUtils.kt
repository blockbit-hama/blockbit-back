package com.sg.service.wallet.ethereum

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.SecureRandom
import java.security.Security

/**
 * MPC (Multi-Party Computation) 유틸리티 클래스
 * Shamir's Secret Sharing을 기반으로 한 키 분산 및 결합 구현
 */
object MpcUtils {

    init {
        // BouncyCastle 프로바이더 추가
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Shamir's Secret Sharing을 사용한 키 분산
     * 
     * @param secret 분산할 비밀 키 (BigInteger)
     * @param totalShares 총 공유 수 (n)
     * @param threshold 임계값 (t) - 복원에 필요한 최소 공유 수
     * @return 키 공유들의 리스트
     */
    fun splitSecret(secret: BigInteger, totalShares: Int, threshold: Int): List<SecretShare> {
        require(threshold <= totalShares) { "임계값은 총 공유 수보다 작거나 같아야 합니다" }
        require(threshold >= 2) { "임계값은 최소 2 이상이어야 합니다" }

        // secp256k1 곡선의 prime order
        val prime = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
        
        // 다항식 계수 생성 (threshold-1 차 다항식)
        val coefficients = mutableListOf<BigInteger>()
        coefficients.add(secret) // a0 = secret
        
        val random = SecureRandom()
        for (i in 1 until threshold) {
            coefficients.add(BigInteger(256, random).mod(prime))
        }

        // 각 x값에 대해 y값 계산 (y = f(x))
        val shares = mutableListOf<SecretShare>()
        for (x in 1..totalShares) {
            var y = BigInteger.ZERO
            var xPower = BigInteger.ONE
            
            for (coeff in coefficients) {
                y = y.add(coeff.multiply(xPower)).mod(prime)
                xPower = xPower.multiply(BigInteger.valueOf(x.toLong())).mod(prime)
            }
            
            shares.add(SecretShare(x, y))
        }

        return shares
    }

    /**
     * Shamir's Secret Sharing을 사용한 비밀 복원
     * 
     * @param shares 키 공유들 (최소 threshold 개수 필요)
     * @return 복원된 비밀 키
     */
    fun combineShares(shares: List<SecretShare>): BigInteger {
        require(shares.size >= 2) { "비밀 복원에는 최소 2개의 공유가 필요합니다" }

        val prime = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
        
        var secret = BigInteger.ZERO

        // Lagrange 보간법 사용
        for (i in shares.indices) {
            var numerator = BigInteger.ONE
            var denominator = BigInteger.ONE

            for (j in shares.indices) {
                if (i != j) {
                    numerator = numerator.multiply(BigInteger.valueOf(-shares[j].x.toLong())).mod(prime)
                    denominator = denominator.multiply(
                        BigInteger.valueOf(shares[i].x.toLong() - shares[j].x.toLong())
                    ).mod(prime)
                }
            }

            // 모듈러 역원 계산
            val lagrangeCoeff = numerator.multiply(denominator.modInverse(prime)).mod(prime)
            secret = secret.add(shares[i].y.multiply(lagrangeCoeff)).mod(prime)
        }

        return secret.mod(prime)
    }

    /**
     * 이더리움 키 페어 생성
     */
    fun generateEthereumKeyPair(): ECKeyPair {
        return Keys.createEcKeyPair()
    }

    /**
     * 공개키로부터 이더리움 주소 계산
     */
    fun computeEthereumAddress(publicKey: BigInteger): String {
        return "0x" + Keys.getAddress(publicKey)
    }

    /**
     * 개인키로부터 공개키 계산
     */
    fun computePublicKey(privateKey: BigInteger): BigInteger {
        // ECKeyPair를 사용한 안전한 방법
        val keyPair = ECKeyPair.create(privateKey)
        return keyPair.publicKey
    }

    /**
     * 16진수 문자열을 BigInteger로 변환
     */
    fun hexToBigInteger(hex: String): BigInteger {
        val cleanHex = if (hex.startsWith("0x")) hex.substring(2) else hex
        return BigInteger(cleanHex, 16)
    }

    /**
     * BigInteger를 16진수 문자열로 변환
     */
    fun bigIntegerToHex(value: BigInteger): String {
        return "0x" + value.toString(16).padStart(64, '0')
    }

    /**
     * 키 공유 데이터 클래스
     */
    data class SecretShare(
        val x: Int,      // x 좌표 (공유 인덱스)
        val y: BigInteger // y 좌표 (공유 값)
    ) {
        fun toHexString(): String {
            return "$x:${bigIntegerToHex(y)}"
        }

        companion object {
            fun fromHexString(hexString: String): SecretShare {
                val parts = hexString.split(":")
                return SecretShare(parts[0].toInt(), hexToBigInteger(parts[1]))
            }
        }
    }
}

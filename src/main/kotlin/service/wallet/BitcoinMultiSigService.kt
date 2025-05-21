package com.sg.service.wallet

import com.sg.dto.wallet.MultisigWalletDTO
import com.sg.dto.wallet.PartiallySignedTransactionDTO
import com.sg.service.wallet.bitcoin.BlockCypherClient
import com.sg.service.wallet.bitcoin.UTXOInfo
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class BitcoinMultiSigService(
    private val apiBaseUrl: String = "https://api.blockcypher.com/v1/btc/test3",
    private val apiKey: String = ""
) {

    private val logger = LoggerFactory.getLogger(BitcoinMultiSigService::class.java)
    private val params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
    private val blockCypherClient = BlockCypherClient(apiBaseUrl, apiKey)
    
    /**
     * 2-of-3 멀티시그 지갑 생성
     */
    fun createMultisigWallet(): MultisigWalletDTO {
        try {
            // 3개의 키 쌍 생성
            val keys = ArrayList<ECKey>()
            val publicKeys = ArrayList<String>()
            val privateKeys = ArrayList<String>()

            for (i in 0 until 3) {
                val key = ECKey()
                keys.add(key)
                publicKeys.add(key.publicKeyAsHex)
                privateKeys.add(key.privateKeyAsHex)
            }

            // 2-of-3 멀티시그 스크립트 생성
            val redeemScript = ScriptBuilder.createMultiSigOutputScript(2, keys)

            // P2SH 주소 생성
            val multiSigAddress = ScriptBuilder.createP2SHOutputScript(redeemScript).getToAddress(params)

            // 결과 반환
            return MultisigWalletDTO().apply {
                address = multiSigAddress.toString()
                this.redeemScript = Utils.HEX.encode(redeemScript.program)
                this.publicKeys = publicKeys
                this.privateKeys = privateKeys // 실제로는 서버에 저장하지 말고 안전하게 분배
            }
        } catch (e: Exception) {
            logger.error("멀티시그 지갑 생성 오류", e)
            throw RuntimeException("멀티시그 지갑 생성 실패", e)
        }
    }

    /**
     * 멀티시그 트랜잭션 생성 (서명 1개 포함)
     */
    fun createMultisigTransaction(
        fromAddress: String,
        toAddress: String,
        amountSatoshi: Long,
        redeemScriptHex: String,
        privateKeyHex: String
    ): PartiallySignedTransactionDTO {

        try {
            logger.info("트랜잭션 생성 시작: 발신 주소=${fromAddress}, 수신 주소=${toAddress}, 금액=${amountSatoshi}")
            
            // 입력값 유효성 검사
            if (privateKeyHex.isNullOrBlank()) {
                throw IllegalArgumentException("개인키가 비어 있습니다.")
            }
            
            // 개인키 로드: 16진수 문자열이 잘못되었을 수 있음
            val privateKey = try {
                logger.debug("개인키 길이: ${privateKeyHex.length}")
                
                // 16진수 문자열에서 공백과 0x 접두사 제거
                val cleanedPrivateKeyHex = privateKeyHex.trim().replace("0x", "")
                
                if (cleanedPrivateKeyHex.length % 2 != 0) {
                    // 길이가 짝수가 아니면 앞에 0 추가
                    "0$cleanedPrivateKeyHex"
                } else {
                    cleanedPrivateKeyHex
                }
                
                // 처리된 16진수 문자열로 개인키 생성
                val keyBytes = Utils.HEX.decode(cleanedPrivateKeyHex)
                logger.debug("키 바이트 길이: ${keyBytes.size}")
                
                ECKey.fromPrivate(keyBytes)
            } catch (e: Exception) {
                logger.error("개인키 파싱 오류: ${e.message}", e)
                throw IllegalArgumentException("잘못된 개인키 형식입니다: ${e.message}")
            }
            
            // 리딤 스크립트 파싱
            val redeemScript = try {
                Script(Utils.HEX.decode(redeemScriptHex))
            } catch (e: Exception) {
                logger.error("리딤 스크립트 파싱 오류: ${e.message}", e)
                throw IllegalArgumentException("잘못된 리딤 스크립트 형식입니다: ${e.message}")
            }
            
            // UTXO 목록 조회
            val utxos = blockCypherClient.getUTXOs(fromAddress)
            if (utxos.isEmpty()) {
                throw RuntimeException("사용 가능한 UTXO가 없습니다. 테스트넷 코인이 있는지 확인하세요.")
            }
            
            logger.info("UTXO 개수: ${utxos.size}, 총 금액: ${utxos.sumOf { it.value }}")
            
            // 수수료 계산 (평균 150 바이트 * 추천 수수료율)
            val feePerByte = blockCypherClient.getRecommendedFee()
            val estimatedFee = 150 * feePerByte
            
            // 필요한 UTXO 선택 (간단한 구현으로 모든 UTXO 사용)
            val totalInput = utxos.sumOf { it.value }
            val change = totalInput - amountSatoshi - estimatedFee
            
            if (change < 0) {
                throw RuntimeException("잔액이 부족합니다. 필요: ${amountSatoshi + estimatedFee} satoshi, 보유: $totalInput satoshi")
            }

            // 트랜잭션 생성
            val tx = Transaction(params)

            // 입력 추가
            for (utxo in utxos) {
                val outPoint = TransactionOutPoint(params, utxo.outputIndex.toLong(), Sha256Hash.wrap(utxo.txId))
                val input = TransactionInput(params, tx, ByteArray(0), outPoint, Coin.valueOf(utxo.value))
                tx.addInput(input)
            }

            // 출력 추가 (받는 주소로)
            tx.addOutput(Coin.valueOf(amountSatoshi), Address.fromString(params, toAddress))
            
            // 거스름돈 출력 추가 (필요한 경우)
            if (change > 0) {
                tx.addOutput(Coin.valueOf(change), Address.fromString(params, fromAddress))
            }

            // 첫 번째 입력에 대한 서명 생성
            val sigHash = tx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false)
            val signature = privateKey.sign(sigHash)
            val txSig = TransactionSignature(signature, Transaction.SigHash.ALL, false)

            // 부분적으로 서명된 트랜잭션 반환
            return PartiallySignedTransactionDTO().apply {
                transactionHex = Utils.HEX.encode(tx.bitcoinSerialize())
                signatureHex = Utils.HEX.encode(txSig.encodeToBitcoin())
                publicKeyHex = privateKey.publicKeyAsHex
                this.redeemScriptHex = redeemScriptHex
            }
        } catch (e: Exception) {
            logger.error("트랜잭션 생성 오류", e)
            throw RuntimeException("트랜잭션 생성 실패: ${e.message}", e)
        }
    }

    /**
     * 멀티시그 트랜잭션 서명 추가 (두 번째 서명)
     */
    fun addSignatureToTransaction(
        partialTx: PartiallySignedTransactionDTO,
        privateKeyHex: String
    ): String {

        try {
            // 개인키 로드 - 첫 번째 메소드와 동일한 방식으로 처리
            val privateKey = try {
                // 16진수 문자열에서 공백과 0x 접두사 제거
                val cleanedPrivateKeyHex = privateKeyHex.trim().replace("0x", "")
                
                val finalKeyHex = if (cleanedPrivateKeyHex.length % 2 != 0) {
                    // 길이가 짝수가 아니면 앞에 0 추가
                    "0$cleanedPrivateKeyHex"
                } else {
                    cleanedPrivateKeyHex
                }
                
                ECKey.fromPrivate(Utils.HEX.decode(finalKeyHex))
            } catch (e: Exception) {
                logger.error("개인키 파싱 오류: ${e.message}", e)
                throw IllegalArgumentException("잘못된 개인키 형식입니다: ${e.message}")
            }
            
            // 트랜잭션 복원
            val txBytes = Utils.HEX.decode(partialTx.transactionHex)
            val tx = Transaction(params, txBytes)

            // 리딤 스크립트 복원
            val redeemScript = Script(Utils.HEX.decode(partialTx.redeemScriptHex))

            // 모든 입력에 서명 적용
            for (i in 0 until tx.inputs.size) {
                // 각 입력에 대한 서명 해시 생성
                val sigHash = tx.hashForSignature(i, redeemScript, Transaction.SigHash.ALL, false)
                
                // 두 번째 서명 생성
                val sig2 = privateKey.sign(sigHash)
                val txSig2 = TransactionSignature(sig2, Transaction.SigHash.ALL, false)

                // 첫 번째 서명 복원
                val sig1Bytes = Utils.HEX.decode(partialTx.signatureHex)
                val txSig1 = TransactionSignature.decodeFromBitcoin(sig1Bytes, false, true) // requireCanonical 추가

                // 서명 스크립트 생성 (OP_0 <sig1> <sig2> <redeemScript>)
                val inputScript = ScriptBuilder.createP2SHMultiSigInputScript(
                    listOf(txSig1, txSig2),
                    redeemScript
                )

                // 트랜잭션에 서명 스크립트 설정
                tx.inputs[i].scriptSig = inputScript
            }

            // 트랜잭션 브로드캐스트
            val txHex = Utils.HEX.encode(tx.bitcoinSerialize())
            val txId = blockCypherClient.broadcastTransaction(txHex)
                ?: throw RuntimeException("트랜잭션 브로드캐스트 실패")

            // 트랜잭션 ID 반환
            return txId
        } catch (e: Exception) {
            logger.error("트랜잭션 서명 추가 오류", e)
            throw RuntimeException("트랜잭션 서명 추가 실패: ${e.message}", e)
        }
    }
    
    /**
     * 트랜잭션 상태 조회
     */
    fun getTransactionStatus(txId: String): String {
        // 간단한 구현: 트랜잭션이 존재하면 "확인됨", 아니면 "미확인"
        return "확인 중"
    }
    
    /**
     * 주소의 UTXO 목록 조회
     * @param address 조회할 비트코인 주소
     * @return UTXO 목록 및 총 잔액 정보를 담은 DTO
     */
    fun getAddressUTXOs(address: String): com.sg.dto.wallet.UTXOResponseDTO {
        try {
            logger.info("UTXO 조회 시작: 주소=${address}")
            
            // UTXO 목록 조회
            val utxos = blockCypherClient.getUTXOs(address)
            
            // 총 잔액 계산
            val totalBalance = utxos.sumOf { it.value }
            
            // UTXO 정보를 DTO로 변환
            val utxoItems = utxos.map { utxo ->
                com.sg.dto.wallet.UTXOItemDTO(
                    txId = utxo.txId,
                    outputIndex = utxo.outputIndex,
                    value = utxo.value,
                    valueBTC = String.format("%.8f", utxo.value.toDouble() / 100000000.0),
                    script = utxo.script
                )
            }
            
            logger.info("UTXO 조회 완료: 개수=${utxos.size}, 총 금액=${totalBalance} satoshi")
            
            // 결과 DTO 반환
            return com.sg.dto.wallet.UTXOResponseDTO(
                address = address,
                utxoCount = utxos.size,
                totalBalance = totalBalance,
                totalBalanceBTC = String.format("%.8f", totalBalance.toDouble() / 100000000.0),
                utxos = utxoItems
            )
        } catch (e: Exception) {
            logger.error("UTXO 조회 오류", e)
            throw RuntimeException("UTXO 조회 실패: ${e.message}", e)
        }
    }
}
package com.sg.service.wallet

import com.sg.dto.wallet.MultisigWalletDTO
import com.sg.dto.wallet.PartiallySignedTransactionDTO
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.slf4j.LoggerFactory

class BitcoinMultiSigService {

    private val logger = LoggerFactory.getLogger(BitcoinMultiSigService::class.java)
    private val params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET)

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
            // 이전 트랜잭션 정보 (실제로는 UTXO 조회 필요)
            // 예시 목적으로 하드코딩 - 실제 구현에서는 API로 UTXO 조회
            val prevTxId = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"
            val prevOutputIndex = 0
            val prevOutputValue = amountSatoshi + 1000 // 수수료 포함

            // 리딤 스크립트 파싱
            val redeemScript = Script(Utils.HEX.decode(redeemScriptHex))

            // 개인키 로드
            val privateKey = ECKey.fromPrivate(Utils.HEX.decode(privateKeyHex))

            // 트랜잭션 생성
            val tx = Transaction(params)

            // 입력 추가
            val outPoint = TransactionOutPoint(params, prevOutputIndex.toLong(), Sha256Hash.wrap(prevTxId))
            val input = TransactionInput(params, tx, ByteArray(0), outPoint, Coin.valueOf(prevOutputValue))
            tx.addInput(input)

            // 출력 추가 (받는 주소로)
            tx.addOutput(Coin.valueOf(amountSatoshi), Address.fromString(params, toAddress))

            // 서명 (1개)
            val txInput = tx.getInput(0)
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
            throw RuntimeException("트랜잭션 생성 실패", e)
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
            // 트랜잭션 복원
            val txBytes = Utils.HEX.decode(partialTx.transactionHex)
            val tx = Transaction(params, txBytes)

            // 개인키 로드
            val privateKey = ECKey.fromPrivate(Utils.HEX.decode(privateKeyHex))

            // 리딤 스크립트 복원
            val redeemScript = Script(Utils.HEX.decode(partialTx.redeemScriptHex))

            // 두 번째 서명 생성
            val input = tx.getInput(0)
            val sigHash = tx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false)
            val sig2 = privateKey.sign(sigHash)
            val txSig2 = TransactionSignature(sig2, Transaction.SigHash.ALL, false)

            // 첫 번째 서명 복원
            val sig1Bytes = Utils.HEX.decode(partialTx.signatureHex)
            val txSig1 = TransactionSignature.decodeFromBitcoin(sig1Bytes, false, true) // requireCanonical 추가

            // 첫 번째 서명자의 공개키
            val pubKey1Bytes = Utils.HEX.decode(partialTx.publicKeyHex)
            val pubKey1 = ECKey.fromPublicOnly(pubKey1Bytes)

            // 서명 스크립트 생성 (OP_0 <sig1> <sig2> <redeemScript>)
            val inputScript = ScriptBuilder.createP2SHMultiSigInputScript(
                listOf(txSig1, txSig2),
                redeemScript
            )

            // 트랜잭션에 서명 스크립트 설정
            input.scriptSig = inputScript

            // 완성된 트랜잭션 직렬화
            return Utils.HEX.encode(tx.bitcoinSerialize())
        } catch (e: Exception) {
            logger.error("트랜잭션 서명 추가 오류", e)
            throw RuntimeException("트랜잭션 서명 추가 실패", e)
        }
    }
}
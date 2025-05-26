package com.sg.controller.wallet

import com.sg.dto.wallet.*
import com.sg.service.wallet.BitcoinMultiSigService
import com.sg.service.wallet.EthereumMpcService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.walletRoutes(
    bitcoinMultiSigService: BitcoinMultiSigService,
    ethereumMpcService: EthereumMpcService
) {
    route("/api/wallet") {
        route("/bitcoin") {
            // 비트코인 멀티시그 지갑 생성
            post("/create") {
                try {
                    val wallet = bitcoinMultiSigService.createMultisigWallet()
                    call.respond(HttpStatusCode.Created, wallet)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create Bitcoin wallet: ${e.message}"))
                }
            }

            // 비트코인 멀티시그 트랜잭션 생성 (첫 번째 서명)
            post("/transaction/create") {
                try {
                    val request = call.receive<BitcoinTransactionRequestDTO>()
                    val tx = bitcoinMultiSigService.createMultisigTransaction(
                        request.fromAddress,
                        request.toAddress,
                        request.amountSatoshi,
                        request.redeemScriptHex,
                        request.privateKeyHex
                    )
                    call.respond(HttpStatusCode.OK, tx)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create Bitcoin transaction: ${e.message}"))
                }
            }

            // 비트코인 멀티시그 트랜잭션 완료 (두 번째 서명)
            post("/transaction/complete") {
                try {
                    val request = call.receive<BitcoinCompleteRequestDTO>()
                    val txId = bitcoinMultiSigService.addSignatureToTransaction(
                        request.partiallySignedTransaction,
                        request.privateKeyHex
                    )
                    call.respond(HttpStatusCode.OK, mapOf(
                        "txId" to txId,
                        "message" to "Transaction successfully broadcasted to the Bitcoin testnet",
                        "explorerUrl" to "https://mempool.space/testnet/tx/$txId"
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to complete Bitcoin transaction: ${e.message}"))
                }
            }

            // 트랜잭션 상태 조회
            get("/transaction/{txId}") {
                try {
                    val txId = call.parameters["txId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Transaction ID is required"))

                    val status = bitcoinMultiSigService.getTransactionStatus(txId)
                    call.respond(HttpStatusCode.OK, mapOf(
                        "txId" to txId,
                        "status" to status,
                        "explorerUrl" to "https://mempool.space/testnet/tx/$txId"
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get transaction status: ${e.message}"))
                }
            }

            // 주소의 UTXO 목록 조회
            get("/utxos/{address}") {
                try {
                    val address = call.parameters["address"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Bitcoin address is required"))

                    val utxoInfo = bitcoinMultiSigService.getAddressUTXOs(address)
                    call.respond(HttpStatusCode.OK, utxoInfo)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve UTXOs: ${e.message}"))
                }
            }
        }

        route("/ethereum") {
            // 이더리움 MPC 지갑 생성 (인증 필요)
            authenticate("jwt-auth") {
                post("/create") {
                    try {
                        // JWT에서 사용자 번호 가져오기
                        val principal = call.principal<JWTPrincipal>()
                        val userNum = principal?.getClaim("usiNum", Int::class) ?: return@post call.respond(
                            HttpStatusCode.Unauthorized, mapOf("error" to "Invalid authentication token"))

                        // 요청 바디에서 추가 정보 받기 (선택사항)
                        val createRequest = try {
                            call.receive<MpcWalletCreateRequestDTO>()
                        } catch (e: Exception) {
                            // 요청 바디가 없거나 파싱 실패 시 기본값 사용
                            MpcWalletCreateRequestDTO()
                        }

                        val walletName = createRequest.walletName ?: "MPC Ethereum Wallet"
                        val astId = createRequest.astId ?: 2  // 이더리움 기본값: 2
                        val polId = 1  // MPC 정책 기본값: 1

                        val wallet = ethereumMpcService.createMpcWallet(userNum, walletName, astId, polId)
                        call.respond(HttpStatusCode.Created, wallet)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create Ethereum wallet: ${e.message}"))
                    }
                }
            }

            // 이더리움 MPC 트랜잭션 생성 (첫 번째 서명)
            post("/transaction/create") {
                try {
                    val request = call.receive<EthereumTransactionRequestDTO>()
                    val partialSig = ethereumMpcService.createPartialSignature(
                        request.walletId,
                        request.participantIndex,
                        request.fromAddress,
                        request.toAddress,
                        request.amount
                    )
                    call.respond(HttpStatusCode.OK, partialSig)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create Ethereum transaction: ${e.message}"))
                }
            }

            // 이더리움 MPC 트랜잭션 완료 (두 번째 서명)
            post("/transaction/complete") {
                try {
                    val request = call.receive<EthereumCompleteRequestDTO>()
                    val txHash = ethereumMpcService.completeAndSubmitTransaction(
                        request.firstSignature,
                        request.secondParticipantIndex
                    )
                    call.respond(HttpStatusCode.OK, mapOf(
                        "txHash" to txHash,
                        "message" to "Transaction successfully submitted to the Ethereum network",
                        "explorerUrl" to "https://sepolia.etherscan.io/tx/$txHash"
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to complete Ethereum transaction: ${e.message}"))
                }
            }

            // 이더리움 잔액 조회
            get("/balance/{address}") {
                try {
                    val address = call.parameters["address"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Address parameter is required"))

                    val balance = ethereumMpcService.getBalance(address)
                    call.respond(HttpStatusCode.OK, mapOf(
                        "address" to address,
                        "balance" to balance.toString(), // BigDecimal을 String으로 변환
                        "unit" to "ETH"
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get Ethereum balance: ${e.message}"))
                }
            }

            // 이더리움 MPC 지갑 정보 조회 (인증 필요)
            authenticate("jwt-auth") {
                get("/info/{walletId}") {
                    try {
                        val walletId = call.parameters["walletId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest, mapOf("error" to "Wallet ID is required"))

                        // TODO: 지갑 정보 조회 서비스 구현
                        call.respond(HttpStatusCode.OK, mapOf(
                            "walletId" to walletId,
                            "message" to "Wallet info retrieval not yet implemented"
                        ))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get wallet info: ${e.message}"))
                    }
                }
            }
        }
    }
}
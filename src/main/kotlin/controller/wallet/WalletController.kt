package com.sg.controller.wallet

import com.sg.dto.wallet.BitcoinCompleteRequestDTO
import com.sg.dto.wallet.BitcoinTransactionRequestDTO
import com.sg.dto.wallet.EthereumCompleteRequestDTO
import com.sg.dto.wallet.EthereumTransactionRequestDTO
import com.sg.service.wallet.BitcoinMultiSigService
import com.sg.service.wallet.EthereumMpcService
import io.ktor.http.*
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
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create Bitcoin wallet: ${e.message}")
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
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create Bitcoin transaction: ${e.message}")
                }
            }

            // 비트코인 멀티시그 트랜잭션 완료 (두 번째 서명)
            post("/transaction/complete") {
                try {
                    val request = call.receive<BitcoinCompleteRequestDTO>()
                    val signedTx = bitcoinMultiSigService.addSignatureToTransaction(
                        request.partiallySignedTransaction,
                        request.privateKeyHex
                    )
                    call.respond(HttpStatusCode.OK, signedTx)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to complete Bitcoin transaction: ${e.message}")
                }
            }
        }

        route("/ethereum") {
            // 이더리움 MPC 지갑 생성
            post("/create") {
                try {
                    val wallet = ethereumMpcService.createMpcWallet()
                    call.respond(HttpStatusCode.Created, wallet)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create Ethereum wallet: ${e.message}")
                }
            }

            // 이더리움 MPC 트랜잭션 생성 (첫 번째 서명)
            post("/transaction/create") {
                try {
                    val request = call.receive<EthereumTransactionRequestDTO>()
                    val partialSig = ethereumMpcService.createPartialSignature(
                        request.walletId,
                        request.participantIndex,
                        request.toAddress,
                        request.amount
                    )
                    call.respond(HttpStatusCode.OK, partialSig)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create Ethereum transaction: ${e.message}")
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
                    call.respond(HttpStatusCode.OK, txHash)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to complete Ethereum transaction: ${e.message}")
                }
            }

            // 이더리움 잔액 조회
            get("/balance/{address}") {
                try {
                    val address = call.parameters["address"]
                    if (address == null) {
                        call.respond(HttpStatusCode.BadRequest, "Address parameter is required")
                        return@get
                    }
                    val balance = ethereumMpcService.getBalance(address)
                    call.respond(HttpStatusCode.OK, balance)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get Ethereum balance: ${e.message}")
                }
            }
        }
    }
}
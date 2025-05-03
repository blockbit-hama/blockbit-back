package com.sg.controller

import com.sg.dto.TransactionDTO
import com.sg.service.TransactionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transactionRoutes(transactionService: TransactionService) {
    
    route("/api/transactions") {
        authenticate("jwt-auth") {
            // 모든 트랜잭션 조회
            get {
                try {
                    val transactions = transactionService.getAllTransactions()
                    call.respond(HttpStatusCode.OK, transactions)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get transactions: ${e.message}")
                }
            }
            
            // 특정 트랜잭션 조회 (승인 정보 포함)
            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid transaction ID is required")
                    
                    val transaction = transactionService.getTransactionDetails(id)
                    if (transaction != null) {
                        call.respond(HttpStatusCode.OK, transaction)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Transaction not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get transaction: ${e.message}")
                }
            }
            
            // 지갑별 트랜잭션 조회
            get("/wallet/{walletId}") {
                try {
                    val walletId = call.parameters["walletId"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid wallet ID is required")
                    
                    val transactions = transactionService.getTransactionsByWalletId(walletId)
                    call.respond(HttpStatusCode.OK, transactions)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get transactions: ${e.message}")
                }
            }
            
            // 트랜잭션 추가
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class)
                    
                    val transactionDTO = call.receive<TransactionDTO>()
                    val trxNum = transactionService.addTransaction(transactionDTO.copy(creusr = usiNum, lmousr = usiNum))
                    call.respond(HttpStatusCode.Created, mapOf("trxNum" to trxNum.toString(), "message" to "Transaction created successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create transaction: ${e.message}")
                }
            }
            
            // 트랜잭션 업데이트
            put {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class)
                    
                    val transactionDTO = call.receive<TransactionDTO>()
                    if (transactionDTO.trxNum == null) {
                        call.respond(HttpStatusCode.BadRequest, "Transaction number is required")
                        return@put
                    }
                    
                    val result = transactionService.updateTransaction(transactionDTO.copy(lmousr = usiNum))
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Transaction updated successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Transaction not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to update transaction: ${e.message}")
                }
            }
            
            // 트랜잭션 상태 업데이트
            put("/{id}/status") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(
                        HttpStatusCode.BadRequest, "Valid transaction ID is required")
                    
                    val status = call.request.queryParameters["status"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest, "Status parameter is required")
                    
                    // 상태값 검증 (pending, confirmed, failed)
                    if (status !in listOf("pending", "confirmed", "failed")) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid status value. Allowed: pending, confirmed, failed")
                        return@put
                    }
                    
                    val result = transactionService.updateTransactionStatus(id, status)
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Transaction status updated successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Transaction not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to update transaction status: ${e.message}")
                }
            }
            
            // 트랜잭션 삭제 (비활성화)
            delete("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, "Valid transaction ID is required")
                    
                    val result = transactionService.deleteTransaction(id)
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Transaction deleted successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Transaction not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete transaction: ${e.message}")
                }
            }
        }
    }
}

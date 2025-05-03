package com.sg.controller

import com.sg.dto.BalanceDTO
import com.sg.service.BalanceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.balanceRoutes(balanceService: BalanceService) {
    
    route("/api/balances") {
        authenticate("jwt-auth") {
            // 모든 잔액 조회
            get {
                try {
                    val balances = balanceService.getAllBalances()
                    call.respond(HttpStatusCode.OK, balances)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get balances: ${e.message}")
                }
            }
            
            // 특정 잔액 조회
            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid balance ID is required")
                    
                    val balance = balanceService.getBalanceById(id)
                    if (balance != null) {
                        call.respond(HttpStatusCode.OK, balance)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Balance not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get balance: ${e.message}")
                }
            }
            
            // 주소별 잔액 조회
            get("/address/{addressId}") {
                try {
                    val addressId = call.parameters["addressId"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid address ID is required")
                    
                    val balances = balanceService.getBalancesByAddressId(addressId)
                    call.respond(HttpStatusCode.OK, balances)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get balances: ${e.message}")
                }
            }
            
            // 자산별 잔액 조회
            get("/asset/{assetId}") {
                try {
                    val assetId = call.parameters["assetId"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid asset ID is required")
                    
                    val balances = balanceService.getBalancesByAssetId(assetId)
                    call.respond(HttpStatusCode.OK, balances)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get balances: ${e.message}")
                }
            }
            
            // 잔액 추가
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class)
                    
                    val balanceDTO = call.receive<BalanceDTO>()
                    val balNum = balanceService.addBalance(balanceDTO.copy(creusr = usiNum, lmousr = usiNum))
                    call.respond(HttpStatusCode.Created, mapOf("balNum" to balNum.toString(), "message" to "Balance created successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create balance: ${e.message}")
                }
            }
            
            // 잔액 업데이트
            put {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class)
                    
                    val balanceDTO = call.receive<BalanceDTO>()
                    if (balanceDTO.balNum == null) {
                        call.respond(HttpStatusCode.BadRequest, "Balance number is required")
                        return@put
                    }
                    
                    val result = balanceService.updateBalance(balanceDTO.copy(lmousr = usiNum))
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Balance updated successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Balance not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to update balance: ${e.message}")
                }
            }
            
            // 잔액 삭제 (비활성화)
            delete("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, "Valid balance ID is required")
                    
                    val result = balanceService.deleteBalance(id)
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Balance deleted successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Balance not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete balance: ${e.message}")
                }
            }
        }
    }
}

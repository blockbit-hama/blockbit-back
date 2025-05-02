package com.sg.controller

import com.sg.dto.*
import com.sg.service.WalletService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.walletRoutes(walletService: WalletService) {
    route("/api/wallets") {
        // 모든 지갑 조회
        get {
            try {
                val wallets = walletService.getAllWallets()
                call.respond(HttpStatusCode.OK, wallets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallets: ${e.message}"))
            }
        }
        
        // 지갑 번호로 조회
        get("/{num}") {
            try {
                val num = call.parameters["num"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid wallet number is required"))
                
                val wallet = walletService.getWalletByNum(num)
                if (wallet != null) {
                    call.respond(HttpStatusCode.OK, wallet)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Wallet not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallet: ${e.message}"))
            }
        }
        
        // 사용자 번호로 지갑 목록 조회
        get("/user/{usiNum}") {
            try {
                val usiNum = call.parameters["usiNum"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid user number is required"))
                
                val wallets = walletService.getWalletsByUser(usiNum)
                call.respond(HttpStatusCode.OK, wallets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallets: ${e.message}"))
            }
        }
        
        // 자산 번호로 지갑 목록 조회
        get("/asset/{astId}") {
            try {
                val astId = call.parameters["astId"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid asset ID is required"))
                
                val wallets = walletService.getWalletsByAsset(astId)
                call.respond(HttpStatusCode.OK, wallets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallets: ${e.message}"))
            }
        }
        
        // 지갑 타입으로 조회
        get("/type/{type}") {
            try {
                val type = call.parameters["type"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Wallet type is required"))
                
                if (!walletService.validateWalletType(type)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid wallet type. Must be 'Self-custody Hot', 'Cold', or 'Trading'"))
                    return@get
                }
                
                val wallets = walletService.getWalletsByType(type)
                call.respond(HttpStatusCode.OK, wallets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallets: ${e.message}"))
            }
        }
        
        // 프로토콜로 조회
        get("/protocol/{protocol}") {
            try {
                val protocol = call.parameters["protocol"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Protocol is required"))
                
                if (!walletService.validateWalletProtocol(protocol)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid protocol. Must be 'MPC' or 'Multisig'"))
                    return@get
                }
                
                val wallets = walletService.getWalletsByProtocol(protocol)
                call.respond(HttpStatusCode.OK, wallets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallets: ${e.message}"))
            }
        }
        
        // 상태로 조회
        get("/status/{status}") {
            try {
                val status = call.parameters["status"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Status is required"))
                
                if (!walletService.validateWalletStatus(status)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid status. Must be 'frozen', 'archived', or 'active'"))
                    return@get
                }
                
                val wallets = walletService.getWalletsByStatus(status)
                call.respond(HttpStatusCode.OK, wallets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get wallets: ${e.message}"))
            }
        }

        // 관리자 권한이 필요한 엔드포인트 (JWT 인증 적용)
        authenticate("jwt-auth") {
            // 신규 지갑 등록
            post {
                try {
                    val walletDTO = call.receive<WalletDTO>()
                    
                    // 지갑 타입 유효성 검사
                    if (!walletService.validateWalletType(walletDTO.walType)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid wallet type. Must be 'Self-custody Hot', 'Cold', or 'Trading'"))
                        return@post
                    }
                    
                    // 프로토콜 유효성 검사
                    if (!walletService.validateWalletProtocol(walletDTO.walProtocol)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid protocol. Must be 'MPC' or 'Multisig'"))
                        return@post
                    }
                    
                    // 상태 유효성 검사
                    if (!walletService.validateWalletStatus(walletDTO.walStatus)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid status. Must be 'frozen', 'archived', or 'active'"))
                        return@post
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val walNum = walletService.addWallet(walletDTO, userNum)
                    call.respond(HttpStatusCode.Created, WalletCreateResponseDTO(walNum, "Wallet created successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to create wallet: ${e.message}"))
                }
            }
            
            // 지갑 정보 업데이트
            put {
                try {
                    val walletDTO = call.receive<WalletDTO>()
                    
                    // 지갑 번호 확인
                    if (walletDTO.walNum == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Wallet number is required"))
                        return@put
                    }
                    
                    // 지갑 타입 유효성 검사
                    if (!walletService.validateWalletType(walletDTO.walType)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid wallet type. Must be 'Self-custody Hot', 'Cold', or 'Trading'"))
                        return@put
                    }
                    
                    // 프로토콜 유효성 검사
                    if (!walletService.validateWalletProtocol(walletDTO.walProtocol)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid protocol. Must be 'MPC' or 'Multisig'"))
                        return@put
                    }
                    
                    // 상태 유효성 검사
                    if (!walletService.validateWalletStatus(walletDTO.walStatus)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid status. Must be 'frozen', 'archived', or 'active'"))
                        return@put
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@put call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = walletService.updateWallet(walletDTO, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Wallet updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Wallet not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to update wallet: ${e.message}"))
                }
            }
            
            // 지갑 정보 부분 업데이트
            patch {
                try {
                    val updateDTO = call.receive<WalletUpdateDTO>()
                    
                    // 지갑 타입 유효성 검사 (있는 경우에만)
                    updateDTO.walType?.let {
                        if (!walletService.validateWalletType(it)) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid wallet type. Must be 'Self-custody Hot', 'Cold', or 'Trading'"))
                            return@patch
                        }
                    }
                    
                    // 프로토콜 유효성 검사 (있는 경우에만)
                    updateDTO.walProtocol?.let {
                        if (!walletService.validateWalletProtocol(it)) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid protocol. Must be 'MPC' or 'Multisig'"))
                            return@patch
                        }
                    }
                    
                    // 상태 유효성 검사 (있는 경우에만)
                    updateDTO.walStatus?.let {
                        if (!walletService.validateWalletStatus(it)) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid status. Must be 'frozen', 'archived', or 'active'"))
                            return@patch
                        }
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@patch call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = walletService.partialUpdateWallet(updateDTO, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Wallet updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Wallet not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to update wallet: ${e.message}"))
                }
            }
            
            // 지갑 상태 업데이트
            put("/status") {
                try {
                    val statusDTO = call.receive<WalletStatusUpdateDTO>()
                    
                    // 상태 유효성 검사
                    if (!walletService.validateWalletStatus(statusDTO.walStatus)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid status. Must be 'frozen', 'archived', or 'active'"))
                        return@put
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@put call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = walletService.updateWalletStatus(statusDTO.walNum, statusDTO.walStatus, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Wallet status updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Wallet not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to update wallet status: ${e.message}"))
                }
            }
            
            // 지갑 삭제 (비활성화)
            delete("/{walNum}") {
                try {
                    val walNum = call.parameters["walNum"]?.toIntOrNull() ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, mapOf("message" to "Valid wallet number is required"))
                    
                    val result = walletService.deleteWallet(walNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Wallet deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Wallet not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to delete wallet: ${e.message}"))
                }
            }
        }
    }
}

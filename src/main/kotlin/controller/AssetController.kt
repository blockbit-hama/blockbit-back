package com.sg.controller

import com.sg.dto.AssetDTO
import com.sg.dto.AssetUpdateDTO
import com.sg.service.AssetService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.assetRoutes(assetService: AssetService) {
    route("/api/assets") {
        // 모든 자산 조회
        get {
            try {
                val assets = assetService.getAllAssets()
                call.respond(HttpStatusCode.OK, assets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get assets: ${e.message}"))
            }
        }
        
        // 자산 번호로 조회
        get("/{num}") {
            try {
                val num = call.parameters["num"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid asset number is required"))
                
                val asset = assetService.getAssetByNum(num)
                if (asset != null) {
                    call.respond(HttpStatusCode.OK, asset)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Asset not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get asset: ${e.message}"))
            }
        }
        
        // 자산 심볼로 조회
        get("/symbol/{symbol}") {
            try {
                val symbol = call.parameters["symbol"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Asset symbol is required"))
                
                val asset = assetService.getAssetBySymbol(symbol)
                if (asset != null) {
                    call.respond(HttpStatusCode.OK, asset)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Asset not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get asset: ${e.message}"))
            }
        }
        
        // 자산 타입으로 조회
        get("/type/{type}") {
            try {
                val type = call.parameters["type"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Asset type is required"))
                
                if (!assetService.validateAssetType(type)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid asset type. Must be 'coin' or 'token'"))
                    return@get
                }
                
                val assets = assetService.getAssetsByType(type)
                call.respond(HttpStatusCode.OK, assets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get assets: ${e.message}"))
            }
        }
        
        // 네트워크로 조회
        get("/network/{network}") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Network is required"))
                
                if (!assetService.validateAssetNetwork(network)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid network. Must be 'mainnet' or 'testnet'"))
                    return@get
                }
                
                val assets = assetService.getAssetsByNetwork(network)
                call.respond(HttpStatusCode.OK, assets)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get assets: ${e.message}"))
            }
        }

        // 관리자 권한이 필요한 엔드포인트 (JWT 인증 적용)
        authenticate("jwt-auth") {
            // 신규 자산 등록
            post {
                try {
                    val assetDTO = call.receive<AssetDTO>()
                    
                    // 자산 타입 유효성 검사
                    if (!assetService.validateAssetType(assetDTO.astType)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid asset type. Must be 'coin' or 'token'"))
                        return@post
                    }
                    
                    // 네트워크 유효성 검사
                    if (!assetService.validateAssetNetwork(assetDTO.astNetwork)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid network. Must be 'mainnet' or 'testnet'"))
                        return@post
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val astNum = assetService.addAsset(assetDTO, userNum)
                    call.respond(HttpStatusCode.Created, mapOf("astNum" to astNum.toString(), "message" to "Asset created successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to create asset: ${e.message}"))
                }
            }
            
            // 자산 정보 업데이트
            put {
                try {
                    val assetDTO = call.receive<AssetDTO>()
                    
                    // 자산 번호 확인
                    if (assetDTO.astNum == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Asset number is required"))
                        return@put
                    }
                    
                    // 자산 타입 유효성 검사
                    if (!assetService.validateAssetType(assetDTO.astType)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid asset type. Must be 'coin' or 'token'"))
                        return@put
                    }
                    
                    // 네트워크 유효성 검사
                    if (!assetService.validateAssetNetwork(assetDTO.astNetwork)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid network. Must be 'mainnet' or 'testnet'"))
                        return@put
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@put call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = assetService.updateAsset(assetDTO, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Asset updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Asset not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to update asset: ${e.message}")
                }
            }
            
            // 자산 정보 부분 업데이트
            patch {
                try {
                    val updateDTO = call.receive<AssetUpdateDTO>()
                    
                    // 자산 타입 유효성 검사 (있는 경우에만)
                    updateDTO.astType?.let {
                        if (!assetService.validateAssetType(it)) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid asset type. Must be 'coin' or 'token'"))
                            return@patch
                        }
                    }
                    
                    // 네트워크 유효성 검사 (있는 경우에만)
                    updateDTO.astNetwork?.let {
                        if (!assetService.validateAssetNetwork(it)) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid network. Must be 'mainnet' or 'testnet'"))
                            return@patch
                        }
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@patch call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = assetService.partialUpdateAsset(updateDTO, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Asset updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Asset not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to update asset: ${e.message}"))
                }
            }
            
            // 자산 삭제 (비활성화)
            delete("/{astNum}") {
                try {
                    val astNum = call.parameters["astNum"]?.toIntOrNull() ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, "Valid asset number is required")
                    
                    val result = assetService.deleteAsset(astNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Asset deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Asset not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to delete asset: ${e.message}"))
                }
            }
        }
    }
}

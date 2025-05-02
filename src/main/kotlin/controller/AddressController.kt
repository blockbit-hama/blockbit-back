package com.sg.controller

import com.sg.dto.AddressDTO
import com.sg.dto.AddressResponseDTO
import com.sg.dto.AddressUpdateDTO
import com.sg.dto.AddressCreateResponseDTO
import com.sg.dto.MessageResponseDTO
import com.sg.service.AddressService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.addressRoutes(addressService: AddressService) {
    route("/api/addresses") {
        // 모든 주소 조회
        get {
            try {
                val addresses = addressService.getAllAddresses()
                call.respond(HttpStatusCode.OK, addresses)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get addresses: ${e.message}"))
            }
        }
        
        // 주소 번호로 조회
        get("/{num}") {
            try {
                val num = call.parameters["num"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid address number is required"))
                
                val address = addressService.getAddressByNum(num)
                if (address != null) {
                    call.respond(HttpStatusCode.OK, address)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Address not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get address: ${e.message}"))
            }
        }
        
        // 지갑 ID로 주소 목록 조회
        get("/wallet/{walId}") {
            try {
                val walId = call.parameters["walId"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid wallet ID is required"))
                
                val addresses = addressService.getAddressesByWallet(walId)
                call.respond(HttpStatusCode.OK, addresses)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get addresses: ${e.message}"))
            }
        }
        
        // 자산 ID로 주소 목록 조회
        get("/asset/{astId}") {
            try {
                val astId = call.parameters["astId"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Valid asset ID is required"))
                
                val addresses = addressService.getAddressesByAsset(astId)
                call.respond(HttpStatusCode.OK, addresses)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get addresses: ${e.message}"))
            }
        }
        
        // 주소 타입으로 조회
        get("/type/{type}") {
            try {
                val type = call.parameters["type"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Address type is required"))
                
                if (!addressService.validateAddressType(type)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid address type. Must be 'receive', 'change', or 'cold'"))
                    return@get
                }
                
                val addresses = addressService.getAddressesByType(type)
                call.respond(HttpStatusCode.OK, addresses)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get addresses: ${e.message}"))
            }
        }
        
        // 실제 암호화폐 주소로 조회
        get("/crypto-address/{address}") {
            try {
                val address = call.parameters["address"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf("message" to "Crypto address is required"))
                
                val addressInfo = addressService.getAddressByAddress(address)
                if (addressInfo != null) {
                    call.respond(HttpStatusCode.OK, addressInfo)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Address not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to get address: ${e.message}"))
            }
        }

        // 관리자 권한이 필요한 엔드포인트 (JWT 인증 적용)
        authenticate("jwt-auth") {
            // 신규 주소 등록
            post {
                try {
                    val addressDTO = call.receive<AddressDTO>()
                    
                    // 주소 타입 유효성 검사
                    if (!addressService.validateAddressType(addressDTO.adrType)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid address type. Must be 'receive', 'change', or 'cold'"))
                        return@post
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val adrNum = addressService.addAddress(addressDTO, userNum)
                    call.respond(HttpStatusCode.Created, AddressCreateResponseDTO(adrNum, "Address created successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to create address: ${e.message}"))
                }
            }
            
            // 주소 정보 업데이트
            put {
                try {
                    val addressDTO = call.receive<AddressDTO>()
                    
                    // 주소 번호 확인
                    if (addressDTO.adrNum == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Address number is required"))
                        return@put
                    }
                    
                    // 주소 타입 유효성 검사
                    if (!addressService.validateAddressType(addressDTO.adrType)) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid address type. Must be 'receive', 'change', or 'cold'"))
                        return@put
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@put call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = addressService.updateAddress(addressDTO, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Address updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Address not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to update address: ${e.message}"))
                }
            }
            
            // 주소 정보 부분 업데이트
            patch {
                try {
                    val updateDTO = call.receive<AddressUpdateDTO>()
                    
                    // 주소 타입 유효성 검사 (있는 경우에만)
                    updateDTO.adrType?.let {
                        if (!addressService.validateAddressType(it)) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid address type. Must be 'receive', 'change', or 'cold'"))
                            return@patch
                        }
                    }
                    
                    // JWT에서 사용자 번호 가져오기
                    val principal = call.principal<JWTPrincipal>()
                    val userNum = principal?.getClaim("usiNum", Int::class) ?: return@patch call.respond(
                        HttpStatusCode.Unauthorized, mapOf("message" to "Invalid authentication token"))
                    
                    val result = addressService.partialUpdateAddress(updateDTO, userNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Address updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Address not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to update address: ${e.message}"))
                }
            }
            
            // 주소 삭제 (비활성화)
            delete("/{adrNum}") {
                try {
                    val adrNum = call.parameters["adrNum"]?.toIntOrNull() ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, mapOf("message" to "Valid address number is required"))
                    
                    val result = addressService.deleteAddress(adrNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, MessageResponseDTO("Address deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponseDTO("Address not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to delete address: ${e.message}"))
                }
            }
        }
    }
}

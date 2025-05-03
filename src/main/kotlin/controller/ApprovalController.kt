package com.sg.controller

import com.sg.dto.ApprovalRequestDTO
import com.sg.service.ApprovalService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.approvalRoutes(approvalService: ApprovalService) {
    
    route("/api/approvals") {
        authenticate("jwt-auth") {
            // 모든 승인 요청 조회
            get {
                try {
                    val approvals = approvalService.getAllApprovals()
                    call.respond(HttpStatusCode.OK, approvals)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get approvals: ${e.message}")
                }
            }
            
            // 특정 승인 요청 조회
            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid approval ID is required")
                    
                    val approval = approvalService.getApprovalById(id)
                    if (approval != null) {
                        call.respond(HttpStatusCode.OK, approval)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Approval not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get approval: ${e.message}")
                }
            }
            
            // 트랜잭션별 승인 요청 조회
            get("/transaction/{transactionId}") {
                try {
                    val transactionId = call.parameters["transactionId"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid transaction ID is required")
                    
                    val approvals = approvalService.getApprovalsByTransactionId(transactionId)
                    call.respond(HttpStatusCode.OK, approvals)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get approvals: ${e.message}")
                }
            }
            
            // 사용자별 승인 요청 조회
            get("/user/{userId}") {
                try {
                    val userId = call.parameters["userId"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Valid user ID is required")
                    
                    val approvals = approvalService.getApprovalsByUserId(userId)
                    call.respond(HttpStatusCode.OK, approvals)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get approvals: ${e.message}")
                }
            }
            
            // 내 승인 요청 조회
            get("/my") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class) ?: return@get call.respond(
                        HttpStatusCode.Unauthorized, "Authentication required")
                    
                    val approvals = approvalService.getApprovalsByUserId(usiNum)
                    call.respond(HttpStatusCode.OK, approvals)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get approvals: ${e.message}")
                }
            }
            
            // 승인 요청 추가
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class)
                    
                    val approvalRequestDTO = call.receive<ApprovalRequestDTO>()
                    val aprNum = approvalService.addApproval(approvalRequestDTO, usiNum)
                    call.respond(HttpStatusCode.Created, mapOf("aprNum" to aprNum.toString(), "message" to "Approval created successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to create approval: ${e.message}")
                }
            }
            
            // 승인 요청 상태 업데이트
            put("/{id}/status") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(
                        HttpStatusCode.BadRequest, "Valid approval ID is required")
                    
                    val status = call.request.queryParameters["status"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest, "Status parameter is required")
                    
                    // 상태값 검증 (pending, approved, rejected)
                    if (status !in listOf("pending", "approved", "rejected")) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid status value. Allowed: pending, approved, rejected")
                        return@put
                    }
                    
                    val comment = call.request.queryParameters["comment"]
                    
                    val principal = call.principal<JWTPrincipal>()
                    val usiNum = principal?.getClaim("usiNum", Int::class)
                    
                    val result = approvalService.updateApprovalStatus(id, status, comment, usiNum)
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Approval status updated successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Approval not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to update approval status: ${e.message}")
                }
            }
            
            // 승인 요청 삭제 (비활성화)
            delete("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, "Valid approval ID is required")
                    
                    val result = approvalService.deleteApproval(id)
                    if (result) {
                        call.respond(HttpStatusCode.OK, "Approval deleted successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Approval not found")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete approval: ${e.message}")
                }
            }
        }
    }
}

package com.sg.controller

import com.sg.dto.*
import com.sg.service.UserInfoService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.Cookie

fun Route.userInfoRoutes(userInfoService: UserInfoService) {
    route("/api/users") {
        // 모든 사용자 조회
        get {
            try {
                val users = userInfoService.getAllUsers()
                call.respond(HttpStatusCode.OK, users)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get users: ${e.message}")
            }
        }
        
        // 사용자 ID로 조회
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, "User ID is required")
                
                val user = userInfoService.getUserById(id)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get user: ${e.message}")
            }
        }
        
        // 사용자 번호로 조회
        get("/num/{num}") {
            try {
                val num = call.parameters["num"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, "Valid user number is required")
                
                val user = userInfoService.getUserByNum(num)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get user: ${e.message}")
            }
        }
        
        // 신규 사용자 등록
        post {
            try {
                val userDTO = call.receive<UserInfoDTO>()
                val usiNum = userInfoService.addUser(userDTO)
                call.respond(HttpStatusCode.Created, mapOf("usiNum" to usiNum.toString(), "message" to "User created successfully"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Failed to create user: ${e.message}")
            }
        }
        
        // 사용자 정보 수정
        put {
            try {
                val userDTO = call.receive<UserInfoDTO>()
                if (userDTO.usiNum == null) {
                    call.respond(HttpStatusCode.BadRequest, "User number is required")
                    return@put
                }
                
                val result = userInfoService.updateUser(userDTO)
                if (result) {
                    call.respond(HttpStatusCode.OK, "User updated successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Failed to update user: ${e.message}")
            }
        }
        
        // 사용자 비밀번호 변경
        put("/change-password") {
            try {
                val passwordDTO = call.receive<ChangePasswordDTO>()
                val result = userInfoService.changePassword(passwordDTO)
                
                if (result) {
                    call.respond(HttpStatusCode.OK, "Password changed successfully")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to change password. Check your current password")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Failed to change password: ${e.message}")
            }
        }
        
        // 사용자 삭제 (비활성화)
        delete("/{usiNum}") {
            try {
                val usiNum = call.parameters["usiNum"]?.toIntOrNull() ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, "Valid user number is required")
                
                val result = userInfoService.deleteUser(usiNum)
                if (result) {
                    call.respond(HttpStatusCode.OK, "User deleted successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete user: ${e.message}")
            }
        }
        
        // 로그인
        post("/login") {
            try {
                val loginDTO = call.receive<LoginDTO>()
                val response = userInfoService.login(loginDTO)
                
                if (response != null && response.success) {
                    // 토큰을 쿠키로 설정
                    response.token?.let { token ->
                        call.response.cookies.append(Cookie(
                            name = "auth_token",
                            value = token,
                            maxAge = environment.config.property("jwt.expiration-time").getString().toInt(),
                            path = "/",
                            secure = false,
                            httpOnly = true
                        ))
                    }
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, response ?: "Login failed")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Failed to login: ${e.message}")
            }
        }
    }
}

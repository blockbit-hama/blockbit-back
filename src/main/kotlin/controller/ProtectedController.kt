package com.sg.controller

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.protectedRoutes() {
    authenticate("jwt-auth") {
        route("/api/protected") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                val userNum = principal?.payload?.getClaim("usiNum")?.asInt()
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "인증된 사용자만 접근할 수 있습니다.",
                    "userId" to userId.toString(),
                    "userNum" to userNum.toString()
                ))
            }
        }
    }
}
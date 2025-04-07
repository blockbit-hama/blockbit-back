package com.sg.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.*

object JwtUtil {
    private lateinit var secretKey: String
    private lateinit var algorithm: Algorithm
    private var expirationTime: Long = 0

    fun init(environment: ApplicationEnvironment) {
        secretKey = environment.config.property("jwt.secret-key").getString()
        expirationTime = environment.config.property("jwt.expiration-time").getString().toLong()
        algorithm = Algorithm.HMAC256(secretKey)
    }

    fun generateToken(usiId: String, usiNum: Int): String {
        return JWT.create()
            .withSubject(usiId)
            .withClaim("usiNum", usiNum)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expirationTime))
            .sign(algorithm)
    }

    fun jwtVerifier(): JWTVerifier {
        return JWT
            .require(algorithm)
            .build()
    }

    fun validateToken(token: String): Boolean {
        return try {
            jwtVerifier().verify(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): String? {
        return try {
            val jwt = jwtVerifier().verify(token)
            jwt.subject
        } catch (e: Exception) {
            null
        }
    }

    fun getUserNumFromToken(token: String): Int? {
        return try {
            val jwt = jwtVerifier().verify(token)
            jwt.getClaim("usiNum").asInt()
        } catch (e: Exception) {
            null
        }
    }
}
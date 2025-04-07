package com.sg

import com.sg.config.factory.DatabaseFactory
import com.sg.config.configureCORS
import com.sg.config.configureHTTPSRedirect
import com.sg.config.configureRouting
import com.sg.config.configureSerialization
import com.sg.plugins.configureJwtAuthentication
import com.sg.utils.JwtUtil
import io.ktor.server.application.*

fun main(args: Array<String> = emptyArray()) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(environment)
    JwtUtil.init(environment)

    configureJwtAuthentication()
    configureRouting()
    configureSerialization()
    configureCORS()
    configureHTTPSRedirect()
}
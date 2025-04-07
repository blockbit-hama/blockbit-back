package com.sg.config

import com.sg.controller.userInfoRoutes
import com.sg.controller.wallet.walletRoutes
import com.sg.controller.protectedRoutes
import com.sg.repository.UserInfoRepository
import com.sg.service.UserInfoService
import com.sg.service.wallet.BitcoinMultiSigService
import com.sg.service.wallet.EthereumMpcService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val userInfoRepository = UserInfoRepository()
    val userInfoService = UserInfoService(userInfoRepository)

    val bitcoinMultiSigService = BitcoinMultiSigService()
    val ethereumMpcService = EthereumMpcService()

    routing {
        get("/") {
            call.respondText("Hello Ktor!")
        }
        protectedRoutes()
        userInfoRoutes(userInfoService)
        walletRoutes(bitcoinMultiSigService, ethereumMpcService)
    }
}
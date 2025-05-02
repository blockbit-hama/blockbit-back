package com.sg.config

import com.sg.controller.userInfoRoutes
import com.sg.controller.wallet.walletRoutes as cryptoWalletRoutes
import com.sg.controller.protectedRoutes
import com.sg.controller.assetRoutes
import com.sg.controller.walletRoutes
import com.sg.repository.UserInfoRepository
import com.sg.repository.AssetRepository
import com.sg.repository.WalletRepository
import com.sg.service.UserInfoService
import com.sg.service.AssetService
import com.sg.service.WalletService
import com.sg.service.wallet.BitcoinMultiSigService
import com.sg.service.wallet.EthereumMpcService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val userInfoRepository = UserInfoRepository()
    val userInfoService = UserInfoService(userInfoRepository)
    
    val assetRepository = AssetRepository()
    val assetService = AssetService(assetRepository)
    
    val walletRepository = WalletRepository()
    val walletService = WalletService(walletRepository)

    val bitcoinMultiSigService = BitcoinMultiSigService()
    val ethereumMpcService = EthereumMpcService()

    routing {
        get("/") {
            call.respondText("Hello Ktor!")
        }
        protectedRoutes()
        userInfoRoutes(userInfoService)
        assetRoutes(assetService)
        walletRoutes(walletService)
        cryptoWalletRoutes(bitcoinMultiSigService, ethereumMpcService)
    }
}
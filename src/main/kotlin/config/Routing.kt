package com.sg.config

import com.sg.controller.*
import com.sg.controller.wallet.walletRoutes as cryptoWalletRoutes
import com.sg.repository.*
import com.sg.service.*
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
    
    val addressRepository = AddressRepository()
    val addressService = AddressService(addressRepository)

    val balanceRepository = BalanceRepository()
    val balanceService = BalanceService(balanceRepository)
    
    val transactionRepository = TransactionRepository()
    val transactionService = TransactionService(transactionRepository)
    
    val approvalRepository = ApprovalRepository()
    val approvalService = ApprovalService(approvalRepository, transactionService)

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
        addressRoutes(addressService)
        balanceRoutes(balanceService)
        transactionRoutes(transactionService)
        approvalRoutes(approvalService)
        cryptoWalletRoutes(bitcoinMultiSigService, ethereumMpcService)
    }
}
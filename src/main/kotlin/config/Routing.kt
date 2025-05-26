package com.sg.config

import com.sg.controller.*
import com.sg.controller.wallet.walletRoutes as cryptoWalletRoutes
import com.sg.repository.*
import com.sg.service.*
import com.sg.service.wallet.BitcoinMultiSigService
import com.sg.service.wallet.EthereumMpcService
import com.sg.service.wallet.SecureKeyStorageService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val bitcoinApiUrl = environment.config.propertyOrNull("bitcoin.api.url")?.getString() ?: "https://api.blockcypher.com/v1/btc/test3"
    val bitcoinApiKey = environment.config.propertyOrNull("bitcoin.api.key")?.getString() ?: ""

    val ethereumInfuraUrl = environment.config.propertyOrNull("ethereum.infura.url")?.getString()
        ?: "https://sepolia.infura.io/v3/49298a1cbcd448d7a20fd0fb7ee12420"

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

    val keyShareRepository = KeyShareRepository()
    val secureKeyStorageService = SecureKeyStorageService(keyShareRepository)

    val bitcoinMultiSigService = BitcoinMultiSigService(bitcoinApiUrl, bitcoinApiKey)
    val ethereumMpcService = EthereumMpcService(
        infuraUrl = ethereumInfuraUrl,
        secureKeyStorageService = secureKeyStorageService,
        walletRepository = walletRepository,
        addressRepository = addressRepository
    )

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
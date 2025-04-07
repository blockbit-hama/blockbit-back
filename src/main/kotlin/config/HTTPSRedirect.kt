package com.sg.config

import io.ktor.server.application.*

fun Application.configureHTTPSRedirect() {
    // 개발 중에는 주석 처리하고, 프로덕션에서만 활성화하는 것이 좋습니다
    // install(HttpsRedirect) {
    //     sslPort = 443
    //     permanentRedirect = true
    // }
}
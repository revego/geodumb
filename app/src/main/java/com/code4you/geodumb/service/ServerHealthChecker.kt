package com.code4you.geodumb.service

import com.code4you.geodumb.api.ApiService
import kotlinx.coroutines.withTimeout


object ServerHealthChecker {
    private var isAlive = true
    private var lastCheck = 0L

    suspend fun update(api: ApiService) {
        isAlive = runCatching {
            withTimeout(2000L) {
                api.healthCheck().isSuccessful
            }
        }.getOrDefault(false)
        lastCheck = System.currentTimeMillis()
    }
    fun isServerAlive(): Boolean {
        return if (System.currentTimeMillis() - lastCheck > 30000) true else isAlive
    }
}
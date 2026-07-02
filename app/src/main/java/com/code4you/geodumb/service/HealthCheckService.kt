package com.code4you.geodumb.service

import android.app.Service
import android.content.Intent
import com.code4you.geodumb.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HealthCheckService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            val api = RetrofitClient.apiService  // ✅ così
            while (true) {
                ServerHealthChecker.update(api)
                delay(30000)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null
}


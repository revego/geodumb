package com.code4you.geodumb.api

import android.content.Context
import android.util.Log
import com.code4you.geodumb.SharedPreferencesHelper
import com.facebook.login.BuildConfig
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object RetrofitClient {

    // Base URL del tuo server API
    //private const val BASE_URL = "https://jsonplaceholder.typicode.com/"
    private const val BASE_URL = "https://api.citylog.cloud/"
    private const val TAG = "RetrofitClient"

    private var retrofit: Retrofit? = null
    private var sharedPreferencesHelper: SharedPreferencesHelper? = null

    // Inizializza RetrofitClient con contesto
    fun initialize(context: Context) {
        if (sharedPreferencesHelper == null) {
            sharedPreferencesHelper = SharedPreferencesHelper(context)
        }
        getRetrofitInstance()
        Log.d(TAG, "RetrofitClient initialized for URL: $BASE_URL")
    }

    private fun getRetrofitInstance(): Retrofit {
        if (retrofit == null) {
            retrofit = buildRetrofit()
        }
        return retrofit!!
    }

    private fun buildRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (Log.isLoggable(TAG, Log.DEBUG)) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            Log.d(TAG, "🔄 Request to: ${originalRequest.url}")

            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            val token = sharedPreferencesHelper?.getAuthToken()
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
                Log.d(TAG, "✅ JWT Token added to request header (first 20): ${token.take(20)}...")
            } else {
                Log.w(TAG, "⚠️ No JWT token available for request")
            }

            val newRequest = requestBuilder.build()
            chain.proceed(newRequest)
        }

        val errorInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response: Response

            try {
                response = chain.proceed(request)

                when (response.code) {
                    401 -> {
                        Log.w(TAG, "🔐 401 Unauthorized - Token expired or invalid")
                        handleUnauthorizedError()
                    }
                    403 -> {
                        Log.w(TAG, "⛔ 403 Forbidden - Insufficient permissions")
                    }
                    500 -> {
                        Log.e(TAG, "💥 500 Internal Server Error")
                    }
                }

                return@Interceptor response

            } catch (e: Exception) {
                Log.e(TAG, "🌐 Network error: ${e.message}")
                throw e
            }
        }

        val urlLogger = Interceptor { chain ->
            val request = chain.request()
            Log.e("RETROFIT_DEBUG", "URL FINALE INVIATA: ${request.url}")
            Log.e("RETROFIT_DEBUG", "Host: ${request.url.host}")
            Log.e("RETROFIT_DEBUG", "Scheme: ${request.url.scheme}")
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    Log.d("DNS_FORCE", "Forzo lookup DNS per $hostname")
                    return Dns.SYSTEM.lookup(hostname)  // usa sempre il DNS del sistema
                }
            })
            .addInterceptor(urlLogger)  // ← mettilo PRIMA degli altri
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(errorInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)  // ← aggiungi anche questo
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun handleUnauthorizedError() {
        Log.w(TAG, "🔐 Token expired or invalid - forcing logout")
        sharedPreferencesHelper?.clearUserData()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun isJwtExpired(jwtToken: String): Boolean {
        return try {
            val parts = jwtToken.split(".")
            if (parts.size < 2) return true

            // Usa Base64.UrlSafe.decode() che supporta direttamente Base64Url
            val payloadBytes = Base64.UrlSafe.decode(parts[1])
            val payload = payloadBytes.decodeToString()
            val json = JSONObject(payload)
            val exp = json.optLong("exp", 0)

            if (exp == 0L) {
                Log.d(TAG, "JWT non ha campo exp")
                return false
            }

            val expiryTime = exp * 1000
            val currentTime = System.currentTimeMillis()
            val isExpired = expiryTime < currentTime

            Log.d(TAG, "JWT exp: $expiryTime, current: $currentTime, expired: $isExpired")
            isExpired
        } catch (e: Exception) {
            Log.e(TAG, "Errore decodifica JWT", e)
            true
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun isJWTValid(): Boolean {
        val token = sharedPreferencesHelper?.getAuthToken() ?: return false

        if (token.startsWith("fake_jwt")) {
            Log.w(TAG, "Fake JWT detected - consider invalid in production")
            return BuildConfig.DEBUG  // Solo in debug accetta fake
        }

        if (token.length < 20 || token.count { it == '.' } != 2) {
            Log.w(TAG, "Invalid JWT format")
            return false
        }

        try {
            val payload = token.split(".")[1]
            val decoded = Base64.UrlSafe.decode(payload).decodeToString()
            val json = JSONObject(decoded)
            val exp = json.optLong("exp", 0L)
            if (exp == 0L) return true  // No exp = assumi valido
            return (exp * 1000) > System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "JWT decode failed", e)
            return false
        }
    }

    fun isJWTValid_(): Boolean {
        val token = sharedPreferencesHelper?.getAuthToken()
        return when {
            token == null -> {
                Log.d(TAG, "❌ No token found")
                false
            }
            token.startsWith("fake_jwt") -> {
                Log.d(TAG, "⚠️ Fake JWT token detected")
                true
            }
            token.length < 20 -> {
                Log.d(TAG, "⚠️ Token too short to be valid JWT")
                false
            }
            else -> {
                Log.d(TAG, "✅ Valid JWT format detected")
                true
            }
        }
    }

    val apiService: ApiService
        get() {
            if (retrofit == null) {
                throw IllegalStateException("RetrofitClient must be initialized first. Call RetrofitClient.initialize(context)")
            }
            return retrofit!!.create(ApiService::class.java)
        }

    fun <T> createService(serviceClass: Class<T>): T {
        return getRetrofitInstance().create(serviceClass)
    }

    fun updateAuthToken(newToken: String) {
        sharedPreferencesHelper?.saveAuthToken(newToken)
        Log.d(TAG, "Auth token updated (first 20): ${newToken.take(20)}...")
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun isAuthenticated(): Boolean {
        val token = sharedPreferencesHelper?.getAuthToken() ?: return false

        // Log di debug (puoi rimuoverlo in produzione o metterlo condizionale)
        Log.d(TAG, "=== IS AUTHENTICATED CHECK ===")
        Log.d(TAG, "Token preview: ${token.take(20)}...")

        // Rifiuta fake token in produzione (opzionale: solo debug)
        if (token.startsWith("fake_jwt")) {
            Log.w(TAG, "Fake JWT detected → considerato non autentico")
            return BuildConfig.DEBUG  // true solo in debug, false in release
        }

        // Controllo base formato JWT
        if (token.length < 20 || token.count { it == '.' } != 2) {
            Log.w(TAG, "Formato JWT non valido")
            return false
        }

        // Controllo scadenza (decodifica payload senza verificare signature)
        try {
            val parts = token.split(".")
            val payloadJson = Base64.UrlSafe.decode(parts[1]).decodeToString()
            val json = JSONObject(payloadJson)

            val exp = json.optLong("exp", 0L)
            if (exp == 0L) {
                Log.d(TAG, "JWT senza campo 'exp' → assumiamo valido")
                return true
            }

            val expiryTime = exp * 1000
            val isExpired = expiryTime < System.currentTimeMillis()

            Log.d(TAG, "JWT exp: $expiryTime ms | now: ${System.currentTimeMillis()} | expired: $isExpired")

            return !isExpired
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante validazione JWT: ${e.message}", e)
            return false
        }
    }

    fun isAuthenticated_(): Boolean  {
        val hasValidToken = sharedPreferencesHelper?.isTokenValid() ?: false
        val isRealJWT = isJWTValid()

        Log.d(TAG, "=== IS AUTHENTICATED DEBUG ===")
        Log.d(TAG, "hasValidToken: $hasValidToken")
        Log.d(TAG, "isRealJWT: $isRealJWT")
        Log.d(TAG, "Token: ${sharedPreferencesHelper?.getAuthToken()?.take(20)}...")
        Log.d(TAG, "Auth check - Has token: $hasValidToken, Is real JWT: $isRealJWT")

        return hasValidToken
        //return hasValidToken && isRealJWT
    }

    fun logout() {
        sharedPreferencesHelper?.clearUserData()
        Log.d(TAG, "User logged out - all credentials cleared")
    }

    fun getCurrentToken(): String? {
        return sharedPreferencesHelper?.getAuthToken()
    }

    fun getCurrentUserData(): SharedPreferencesHelper.UserData? {
        return sharedPreferencesHelper?.getUserData()
    }

    fun getBaseUrl(): String {
        return BASE_URL
    }
}
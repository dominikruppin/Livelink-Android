package com.livelink.data.remote

import com.livelink.data.model.BotRequest
import com.livelink.data.model.BotResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// URL der Perplexity API
const val BASE_URL_BOT = "https://api.perplexity.ai/"

// Wir loggen die Anfragen und Antworten
val botLoggingInterceptor = HttpLoggingInterceptor().also {
    it.level = HttpLoggingInterceptor.Level.BODY
}

val botClient = OkHttpClient.Builder()
    .addInterceptor(botLoggingInterceptor)
    .build()

val botMoshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val botRetrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(botMoshi))
    .baseUrl(BASE_URL_BOT)
    .client(botClient)
    .build()

interface BotApiService {

    // Wir übergeben die Nachricht als JSON-Body.
    // Als Antwort kriegen wir eine strukturierte Antwort der API.
    @Headers(
        "Content-Type: application/json"
    )
    @POST("chat/completions")
    suspend fun sendMessage(@Header("Authorization") apiKey: String, @Body request: BotRequest): BotResponse
}

object BotApi {
    val apiService: BotApiService by lazy { botRetrofit.create(BotApiService::class.java) }
}
package com.livelink.data.remote

import com.livelink.data.model.ZipCodeInfos
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

const val BASE_URL = "https://openplzapi.org/"

val loggingInterceptor = HttpLoggingInterceptor().also {
    it.level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface ZipCodeApiService {

    @GET("{country}/Localities")
    suspend fun getZipInfos(
        @Path("country") country: String,
        @Query("postalCode") zip: String
    ) : List<ZipCodeInfos>


}

object ZipCodeApi {
    val apiService: ZipCodeApiService by lazy { retrofit.create(ZipCodeApiService::class.java) }
}
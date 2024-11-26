package com.example.myfirstjetpackcomposeandroidapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {
    @GET("192.168.100.225") // Địa chỉ IP của ESP8266
    suspend fun getSensorData(): SensorData
}

object RetrofitInstance {
    private const val BASE_URL = "http://192.168.100.225"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}



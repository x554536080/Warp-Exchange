package com.kuma.warpexchange.network

import retrofit2.Call
import retrofit2.http.GET

interface TradingService {

    @GET("api/assets")
    fun getAssets(): Call<String>

}
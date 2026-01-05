package com.kuma.warpexchange.network.service

import com.kuma.warpexchange.model.OrderRequestBean
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TradingService {

    @GET("api/assets")
    fun getAssets(): Call<String>

    @POST("api/orders")
    fun createOrder(@Body orderRequestBean: OrderRequestBean): Call<String>

    @GET("api/orders")
    fun getOpenOrders(): Call<String>

    @GET("api/orderBook")
    fun getOrderBook(): Call<String>
}
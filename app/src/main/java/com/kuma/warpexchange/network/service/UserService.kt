package com.kuma.warpexchange.network.service

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UserService {

    @FormUrlEncoded
    @POST("sign_up_api")
    fun signUp(
        @Field("email") email: String,
        @Field("name") name: String,
        @Field("password") password: String
    ):Call<String>

    @FormUrlEncoded
    @POST("sign_in_api")
    fun signIn(
        @Field("email") email: String,
        @Field("password") password: String
    ):Call<String>
}
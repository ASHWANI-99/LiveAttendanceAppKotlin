package com.agungfir.liveattendanceapp.networking

import com.agungfir.liveattendanceapp.model.LoginResponse
import com.agungfir.liveattendanceapp.model.ForgotPasswordResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LiveAttendaceApiService {

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/login")
    fun loginRequest(@Body body: String): Call<LoginResponse>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/password/forgot")
    fun forgotPassworRequest(@Body body: String): Call<ForgotPasswordResponse>
}
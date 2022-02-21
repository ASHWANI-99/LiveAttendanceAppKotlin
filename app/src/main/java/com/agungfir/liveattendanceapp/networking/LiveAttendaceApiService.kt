package com.agungfir.liveattendanceapp.networking

import com.agungfir.liveattendanceapp.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface LiveAttendaceApiService {

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/login")
    fun loginRequest(@Body body: String): Call<LoginResponse>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/password/forgot")
    fun forgotPassworRequest(@Body body: String): Call<ForgotPasswordResponse>

    @Multipart
    @Headers("Accept: application/json")
    @POST("attendance")
    fun attend(
        @Header("Authorization") token: String,
        @PartMap params: HashMap<String, RequestBody>,
        @Part photo: MultipartBody.Part
    ): Call<AttendanceResponse>

    @Headers("Accept: application/json")
    @GET("attendance/history")
    fun getHistoryAttendance(
        @Header("Authorization") token: String,
        @Query("from") fromDate: String,
        @Query("to") toDate: String
    ): Call<HistoryResponse>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/logout")
    fun logoutRequest(@Header("Authorization") token: String): Call<LogoutResponse>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/password/reset")
    fun changePassRequest(
        @Header("Authorization") token: String,
        @Body body: String
    ): Call<ChangePasswordResponse>
}
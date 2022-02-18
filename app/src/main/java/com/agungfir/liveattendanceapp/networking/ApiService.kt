package com.agungfir.liveattendanceapp.networking

object ApiService {

    fun getLiveAttendanceServices(): LiveAttendaceApiService {
        return RetrofitClient
            .getClient()
            .create(LiveAttendaceApiService::class.java)
    }

}
package com.example.studentapp.data.remote.api

import com.example.studentapp.data.remote.response.TeacherResponse
import retrofit2.http.GET

interface TeacherApi {
    @GET("users?page=1")
    suspend fun getTeachers(): TeacherResponse
}

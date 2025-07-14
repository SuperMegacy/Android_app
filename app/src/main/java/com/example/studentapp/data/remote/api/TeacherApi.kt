package com.example.studentapp.data.remote.api

import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.model.TeacherResponse
import retrofit2.http.GET
import retrofit2.http.Headers


interface TeacherApi {
    @Headers("x-api-key: reqres-free-v1")
    @GET("users?page=1")
    suspend fun getTeachers(): TeacherResponse
}

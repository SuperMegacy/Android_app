package com.example.studentapp.data.remote

import com.example.studentapp.data.remote.api.TeacherApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "https://reqres.in/api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val teacherApi: TeacherApi by lazy {
        retrofit.create(TeacherApi::class.java)
    }
}

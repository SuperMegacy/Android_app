
package com.example.studentapp.data.remote.response

import com.example.studentapp.data.model.Teacher
import com.google.gson.annotations.SerializedName

data class TeacherResponse(
    @SerializedName("data")
    val data: List<Teacher>
)

data class TeacherDto(
    val id: Int,
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val avatar: String? = null
)
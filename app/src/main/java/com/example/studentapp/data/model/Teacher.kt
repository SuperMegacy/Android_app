package com.example.studentapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "teacher")
data class Teacher(
    @PrimaryKey
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "email_id")
    @SerializedName("email")
    val email: String,

    @ColumnInfo(name = "first_name")
    @SerializedName("first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    @SerializedName("last_name")
    val lastName: String
)

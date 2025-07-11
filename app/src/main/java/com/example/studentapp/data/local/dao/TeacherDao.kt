package com.example.studentapp.data.local.dao

import androidx.room.*
import com.example.studentapp.data.model.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    @Query("SELECT * FROM teacher")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teacher WHERE id = :id")
    suspend fun getTeacherById(id: Int): Teacher?

    @Query("SELECT * FROM teacher WHERE email_id = :email")
    suspend fun getTeacherByEmail(email: String): Teacher?
}


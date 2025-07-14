package com.example.studentapp.data.local.dao

import androidx.room.*
import com.example.studentapp.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT * FROM student")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM student WHERE id = :id")
    suspend fun getStudentById(id: Int): Student?

    @Query("SELECT * FROM student WHERE email_id = :email")
    suspend fun getStudentByEmail(email: String): Student?

    @Query("SELECT * FROM student")
    suspend fun getAllStudentsList(): List<Student>

}

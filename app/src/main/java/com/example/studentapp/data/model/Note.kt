package com.example.studentapp.data.model

import androidx.room.*
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.model.Student

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacher_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teacher_id"), Index("student_id")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    @ColumnInfo(name = "teacher_id") val teacherId: Int,
    @ColumnInfo(name = "student_id") val studentId: Int,
    @ColumnInfo(name = "image_urls") val imageUrls: List<String> = emptyList(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

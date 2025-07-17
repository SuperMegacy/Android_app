package com.example.studentapp.data.model

import androidx.room.*

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacher_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("teacher_id"), Index("student_id")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val description: String,

    @ColumnInfo(name = "teacher_id")
    val teacherId: Int,

    @ColumnInfo(name = "student_id")
    val studentId: Int,

    @ColumnInfo(name = "image_urls")
    val imageUrls: List<String> = emptyList(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // Add marks (nullable, for teacher grading)
    var marks: Int? = null
)

package com.example.studentapp.data.local

import android.content.Context
import androidx.room.*
import com.example.studentapp.data.local.converters.Converters
import com.example.studentapp.data.local.dao.NoteDao
import com.example.studentapp.data.local.dao.StudentDao
import com.example.studentapp.data.local.dao.TeacherDao
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.model.Student
import com.example.studentapp.data.model.Teacher

@Database(
    entities = [Note::class, Student::class, Teacher::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_app_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

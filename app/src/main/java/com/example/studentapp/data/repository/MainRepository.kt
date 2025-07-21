package com.example.studentapp.data.repository

import com.example.studentapp.data.local.dao.NoteDao
import com.example.studentapp.data.local.dao.StudentDao
import com.example.studentapp.data.local.dao.TeacherDao
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.model.Student
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.remote.RetrofitInstance
import com.example.studentapp.data.remote.api.TeacherApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainRepository(
    private val noteDao: NoteDao,
    private val studentDao: StudentDao,
    private val teacherDao: TeacherDao
) {

    private val teacherApi: TeacherApi = RetrofitInstance.teacherApi

    // region: Teacher
    suspend fun syncTeachersFromApi() = withContext(Dispatchers.IO) {
        try {
            val response = teacherApi.getTeachers()
            val teachersFromApi = response.data
            teacherDao.insertTeachers(teachersFromApi)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    fun getAllTeachers(): Flow<List<Teacher>> = teacherDao.getAllTeachers()

    // region: Student
    suspend fun insertStudent(student: Student) = withContext(Dispatchers.IO) {
        studentDao.insertStudent(student)
    }

    suspend fun getStudentByEmail(email: String): Student? = withContext(Dispatchers.IO) {
        studentDao.getStudentByEmail(email)
    }



    suspend fun getStudentById(id: Int): Student? = withContext(Dispatchers.IO) {
        studentDao.getStudentById(id)
    }


    // region: Notes
    suspend fun insertNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

//    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
//        noteDao.deleteNote(note)
//    }

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesByStudent(studentId: Int): Flow<List<Note>> = noteDao.getNotesForStudent(studentId)

    fun getNotesByTeacher(teacherId: Int): Flow<List<Note>> = noteDao.getNotesByTeacher(teacherId)

    suspend fun getNoteById(noteId: Int): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(noteId)
    }

    companion object {
        @Volatile
        private var instance: MainRepository? = null

        fun getInstance(
            noteDao: NoteDao,
            studentDao: StudentDao,
            teacherDao: TeacherDao
        ): MainRepository {
            return instance ?: synchronized(this) {
                instance ?: MainRepository(noteDao, studentDao, teacherDao).also {
                    instance = it
                }
            }
        }
    }
}

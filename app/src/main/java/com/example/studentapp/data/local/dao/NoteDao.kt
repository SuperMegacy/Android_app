package com.example.studentapp.data.local.dao

import androidx.room.*
import com.example.studentapp.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE student_id = :studentId ORDER BY created_at DESC")
    fun getNotesForStudent(studentId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE teacher_id = :teacherId ORDER BY created_at DESC")
    fun getNotesByTeacher(teacherId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Int): Note?
}

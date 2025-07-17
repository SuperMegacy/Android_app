package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.*
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map

class NoteListViewModel(private val repository: MainRepository) : ViewModel() {


    private val _noteUpdates = MutableSharedFlow<Note>()
    val noteUpdates: SharedFlow<Note> = _noteUpdates

    // Call this when a note is updated
    suspend fun noteUpdated(note: Note) {
        _noteUpdates.emit(note)
    }

    fun loadStudentNotes(studentId: Int): Flow<List<Note>> {
        return repository.getNotesByStudent(studentId)
    }

    fun loadTeacherNotes(teacherId: Int): Flow<List<Note>> {
        return repository.getNotesByTeacher(teacherId)
    }

    fun getAllNotes(): Flow<List<Note>> {
        return repository.getAllNotes()
    }
}

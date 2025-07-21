package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class NoteListViewModel(private val repository: MainRepository) : ViewModel() {

    // For notifying about note updates
    private val _noteUpdates = MutableSharedFlow<Note>()
    val noteUpdates: SharedFlow<Note> = _noteUpdates

    // For loading state
    private val _isLoading = MutableSharedFlow<Boolean>()
    val isLoading: SharedFlow<Boolean> = _isLoading

    // For error messages
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    /**
     * Load notes for a specific student
     * @param studentId ID of the student
     * @return Flow of notes
     */
    fun loadStudentNotes(studentId: Int): Flow<List<Note>> {
        return repository.getNotesByStudent(studentId)
    }

    /**
     * Load notes for a specific teacher
     * @param teacherId ID of the teacher
     * @return Flow of notes
     */
    fun loadTeacherNotes(teacherId: Int): Flow<List<Note>> {
        return repository.getNotesByTeacher(teacherId)
    }

    /**
     * Load all notes
     * @return Flow of all notes
     */
    fun getAllNotes(): Flow<List<Note>> {
        return repository.getAllNotes()
    }

    /**
     * Notify when a note is updated
     * @param note The updated note
     */
    suspend fun noteUpdated(note: Note) {
        try {
            _noteUpdates.emit(note)
        } catch (e: Exception) {
            _errorMessage.emit("Failed to update note: ${e.message}")
        }
    }

    /**
     * Delete a note
     * @param note The note to delete
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                _isLoading.emit(true)
                repository.deleteNote(note)
            } catch (e: Exception) {
                _errorMessage.emit("Failed to delete note: ${e.message}")
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    private fun MainRepository.deleteNote(note: Note) {}
}
// NoteDetailsViewModel.kt
package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.*
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.repository.MainRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NoteDetailsViewModel(private val repository: MainRepository) : ViewModel() {

    private val _note = MutableLiveData<Note?>()
    val note: LiveData<Note?> get() = _note

    val teachers: LiveData<List<Teacher>> = liveData {
        emit(repository.getAllTeachers().first())
    }

    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            _note.value = repository.getNoteById(noteId)
        }
    }

    suspend fun saveNote(note: Note) {
        if (note.id == 0) {
            repository.insertNote(note)
        } else {
            repository.updateNote(note)
        }
    }
}

class NoteDetailsViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteDetailsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

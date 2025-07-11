package com.example.studentapp.ui.viewmodels

import androidx.lifecycle.*
import com.example.studentapp.data.local.dao.NoteDao
import com.example.studentapp.data.model.Note
import kotlinx.coroutines.launch

class NoteListViewModel(private val noteDao: NoteDao) : ViewModel() {

    // LiveData from Room using Flow converted to LiveData
    val notes: LiveData<List<Note>> = noteDao.getAllNotes().asLiveData()

    // Example: Add note function (optional)
    fun addNote(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note)
        }
    }
}

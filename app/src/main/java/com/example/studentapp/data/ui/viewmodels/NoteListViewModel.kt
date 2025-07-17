package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.*
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.repository.MainRepository

class NoteListViewModel(private val repository: MainRepository) : ViewModel() {

    // Expose all notes as LiveData
    val notes: LiveData<List<Note>> = repository.getAllNotes().asLiveData()

}

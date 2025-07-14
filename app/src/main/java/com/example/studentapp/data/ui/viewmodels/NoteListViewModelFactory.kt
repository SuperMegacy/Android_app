package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studentapp.data.local.dao.NoteDao



class NoteListViewModelFactory(
        private val noteDao: NoteDao
        ) : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(NoteListViewModel::class.java) -> {
                        NoteListViewModel(noteDao) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.studentapp.data.repository.MainRepository

class TeacherListViewModel(
    private val repository: MainRepository
) : ViewModel() {

    // LiveData list of teachers from repository
    val teachers = repository.getAllTeachers().asLiveData()
}

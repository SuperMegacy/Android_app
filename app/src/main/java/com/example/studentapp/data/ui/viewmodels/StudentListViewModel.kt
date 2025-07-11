package com.example.studentapp.data.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StudentListViewModel(application: Application) : AndroidViewModel(application) {

    private val studentDao = AppDatabase.getInstance(application).studentDao()

    val students: Flow<List<Student>> = studentDao.getAllStudents()

    // Optional: if you want to insert or update students from here
    fun insertStudent(student: Student) = viewModelScope.launch {
        studentDao.insertStudent(student)
    }
}

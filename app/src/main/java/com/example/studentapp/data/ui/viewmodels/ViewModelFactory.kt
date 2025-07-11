// File: LoginViewModelFactory.kt
package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studentapp.data.local.dao.TeacherDao
import com.example.studentapp.data.repository.MainRepository

class LoginViewModelFactory(
    private val repository: MainRepository,
    private val teacherDao: TeacherDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository, teacherDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

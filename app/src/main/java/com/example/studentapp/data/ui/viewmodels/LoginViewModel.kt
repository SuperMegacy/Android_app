// LoginViewModel.kt
package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapp.data.repository.MainRepository
import kotlinx.coroutines.launch
import com.example.studentapp.data.model.Student
import com.example.studentapp.data.model.Teacher

class LoginViewModel(private val repository: MainRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Boolean?>(null)
    val loginState: LiveData<Boolean?> get() = _loginState

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun login(username: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Check both student and teacher
                val teacher = repository.getTeacherByEmail(username)
                val student = repository.getStudentByEmail(username)

                val success = when {
                    teacher != null && validatePassword(teacher, password) -> true
                    student != null && validatePassword(student, password) -> true
                    else -> false
                }

                _loginState.postValue(success)
            } catch (e: Exception) {
                _errorMessage.postValue("Login failed: ${e.message}")
                _loginState.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun validatePassword(user: Any, password: String): Boolean {
        return when (user) {
            is Teacher -> user.email == password // Simple validation for demo

            else -> false
        }
    }

    fun resetState() {
        _loginState.value = null
        _errorMessage.value = null
    }
}
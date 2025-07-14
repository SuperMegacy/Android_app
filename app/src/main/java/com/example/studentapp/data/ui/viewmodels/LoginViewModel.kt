package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: MainRepository
) : ViewModel() {

    val teachers: Flow<List<Teacher>> = repository.getAllTeachers()

    private val _loginState = MutableLiveData<Boolean?>(null)
    val loginState: LiveData<Boolean?> get() = _loginState

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    /**
     * Login validation by first and last name only (no password).
     */
    fun login(firstName: String, lastName: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val students = repository.getAllStudentsList()
                val matched = students.any { student ->
                    student.firstName.equals(firstName, ignoreCase = true) &&
                            student.lastName.equals(lastName, ignoreCase = true)
                }
                _loginState.postValue(matched)
            } catch (e: Exception) {
                _errorMessage.postValue("Login failed: ${e.message}")
                _loginState.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun resetState() {
        _loginState.value = null
        _errorMessage.value = null
    }
}

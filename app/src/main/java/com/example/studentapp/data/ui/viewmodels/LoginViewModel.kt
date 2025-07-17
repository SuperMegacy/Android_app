package com.example.studentapp.data.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapp.data.model.Student
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.repository.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _studentId = MutableLiveData<Int?>()
    val studentId: LiveData<Int?> get() = _studentId

    private val _studentName = MutableLiveData<String?>()
    val studentName: LiveData<String?> get() = _studentName

    /**
     * Validate student login by ID and first name
     */
    fun login(studentId: Int, firstName: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val student = repository.getStudentById(studentId)
                withContext(Dispatchers.Main) {
                    if (student != null && student.firstName.equals(firstName, ignoreCase = true)) {
                        _studentId.value = student.id
                        _studentName.value = "${student.firstName} ${student.lastName}"
                        _loginState.value = true
                    } else {
                        _loginState.value = false
                    }
                }
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

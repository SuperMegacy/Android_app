package com.example.studentapp.data.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Teacher

class TeacherListViewModel(application: Application) : AndroidViewModel(application) {

    private val teacherDao = AppDatabase.getInstance(application).teacherDao()

    // Convert Flow to LiveData
    val teachers = teacherDao.getAllTeachers().asLiveData()
}

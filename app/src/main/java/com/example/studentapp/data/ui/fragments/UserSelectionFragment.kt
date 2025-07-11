package com.example.studentapp.data.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Student
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.databinding.FragmentUserSelectionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class UserType {
    TEACHER,
    STUDENT
}

class UserSelectionFragment : Fragment() {

    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: MainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSelectionBinding.inflate(inflater, container, false)

        val database = AppDatabase.getInstance(requireContext())
        repository = MainRepository.getInstance(
            database.noteDao(),
            database.studentDao(),
            database.teacherDao()
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Call API to sync teachers and add dummy students
        CoroutineScope(Dispatchers.IO).launch {
            repository.syncTeachersFromApi()
            addDummyStudents()
        }

        binding.btnLoginTeacher.setOnClickListener {
            navigateToLogin(UserType.TEACHER)
        }

        binding.btnLoginStudent.setOnClickListener {
            navigateToLogin(UserType.STUDENT)
        }
    }

    private fun navigateToLogin(userType: UserType) {
        val action = UserSelectionFragmentDirections.actionUserSelectionFragmentToLoginFragment(userType.name)
        findNavController().navigate(action)
    }

    private suspend fun addDummyStudents() {
        val dummyStudents = listOf(
            Student(0, "Student One", "student1@example.com", "ima"),
            Student(0, "Student Two", "student2@example.com", "mega"),
            Student(0, "Student Three", "student3@example.com", "iam ")
        )

        dummyStudents.forEach { student ->
            val exists = repository.getStudentByEmail(student.email)
            if (exists == null) {
                repository.insertStudent(student)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

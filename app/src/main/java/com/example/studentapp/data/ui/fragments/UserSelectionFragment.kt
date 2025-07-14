package com.example.studentapp.data.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentapp.R
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Student
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.databinding.FragmentUserSelectionBinding
import kotlinx.coroutines.launch

enum class UserType {
    TEACHER,
    STUDENT
}

class UserSelectionFragment : Fragment() {

    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(requireActivity().applicationContext)
        repository = MainRepository.getInstance(
            database.noteDao(),
            database.studentDao(),
            database.teacherDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sync API and prefill dummy students
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.syncTeachersFromApi()
                addDummyStudents()
                Log.d("UserSelectionFragment", "Teachers synced and students added.")
            } catch (e: Exception) {
                Log.e("UserSelectionFragment", "Error syncing data", e)
            }
        }

        binding.btnLoginTeacher.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.userSelectionFragment) {
                navigateToLogin(UserType.TEACHER)
            }
        }

        binding.btnLoginStudent.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.userSelectionFragment) {
                navigateToLogin(UserType.STUDENT)
            }
        }
    }

    private fun navigateToLogin(userType: UserType) {
        try {
            val action = UserSelectionFragmentDirections
                .actionUserSelectionFragmentToLoginFragment(userType.name)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("UserSelectionFragment", "Navigation failed", e)
        }
    }

    private suspend fun addDummyStudents() {
        try {
            val dummyStudents = listOf(
                Student(0, "Student One", "student1@example.com", "ima"),
                Student(0, "Student Two", "student2@example.com", "mega"),
                Student(0, "Student Three", "student3@example.com", "iam ")
            )

            for (student in dummyStudents) {
                val exists = repository.getStudentByEmail(student.email)
                if (exists == null) {
                    repository.insertStudent(student)
                    Log.d("UserSelectionFragment", "Inserted dummy student: ${student.firstName}")
                }
            }
        } catch (e: Exception) {
            Log.e("UserSelectionFragment", "Error inserting dummy students", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

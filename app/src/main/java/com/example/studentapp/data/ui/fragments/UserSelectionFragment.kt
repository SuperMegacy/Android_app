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
import com.example.studentapp.data.model.UserType
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.databinding.FragmentUserSelectionBinding
import kotlinx.coroutines.launch

class UserSelectionFragment : Fragment() {

    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: MainRepository

    // Initialize the repository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = requireActivity().applicationContext
        val database = AppDatabase.getInstance(context)

        repository = MainRepository.getInstance(
            database.noteDao(),
            database.studentDao(),
            database.teacherDao()
        )
    }

    // Inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("UserSelectionFragment", "Syncing teachers from API...")
                repository.syncTeachersFromApi()

                Log.d("UserSelectionFragment", "Inserting dummy students if needed...")
                insertDummyStudentsIfNotExists()

                Log.d("UserSelectionFragment", "✅ Teacher + Student setup complete.")
            } catch (e: Exception) {
                Log.e("UserSelectionFragment", "❌ Error syncing data", e)
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
                .actionUserSelectionFragmentToLoginFragment(userType)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("UserSelectionFragment", "❌ Navigation failed", e)
        }
    }

    private suspend fun insertDummyStudentsIfNotExists() {
        val dummyStudents = listOf(
            Student(1001, "emanuel@example.com", "Emanuel", "Pilusa"),
            Student(1002, "sarah@example.com", "Sarah", "Mokoena"),
            Student(1003, "lebo@example.com", "Lebo", "Mabena")
        )

        for (student in dummyStudents) {
            val exists = repository.getStudentByEmail(student.email)
            if (exists == null) {
                repository.insertStudent(student)
                Log.d("UserSelectionFragment", "Inserted student: ${student.firstName}")
            } else {
                Log.d("UserSelectionFragment", "Student already exists: ${student.firstName}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

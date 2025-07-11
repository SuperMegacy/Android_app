package com.example.studentapp.data.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.viewmodels.LoginViewModel
import com.example.studentapp.data.ui.viewmodels.LoginViewModelFactory
import com.example.studentapp.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LoginViewModel
    private lateinit var repository: MainRepository

    private var loginType: String? = null
    private var teacherList: List<Teacher> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        val database = AppDatabase.getInstance(requireContext())
        repository = MainRepository.getInstance(
            noteDao = database.noteDao(),
            studentDao = database.studentDao(),
            teacherDao = database.teacherDao()
        )

        val factory = LoginViewModelFactory(repository, database.teacherDao())
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        loginType = arguments?.getString("loginType")

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        if (loginType == "TEACHER") {
            // Show Spinner for teachers and hide username/password fields
            binding.spinnerTeachers.visibility = View.VISIBLE
            binding.etUsername.visibility = View.GONE
            binding.etPassword.visibility = View.GONE

            // Load teachers from DB and populate spinner
            lifecycleScope.launch {
                repository.getAllTeachers().collect { teachers ->
                    teacherList = teachers
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        teachers.map { "${it.id} - ${it.firstName} ${it.lastName}" }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerTeachers.adapter = adapter
                }
            }

            binding.btnLogin.setOnClickListener {
                val selectedPosition = binding.spinnerTeachers.selectedItemPosition
                if (selectedPosition == AdapterView.INVALID_POSITION) {
                    Toast.makeText(requireContext(), "Please select a teacher", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedTeacher = teacherList[selectedPosition]
                // You can pass teacher info as needed, for now just proceed
                Toast.makeText(requireContext(), "Logged in as: ${selectedTeacher.firstName}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToNoteListFragment())
            }

        } else { // STUDENT login
            binding.spinnerTeachers.visibility = View.GONE
            binding.etUsername.visibility = View.VISIBLE
            binding.etPassword.visibility = View.VISIBLE

            binding.btnLogin.setOnClickListener {
                val username = binding.etUsername.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.login(username, password)
            }

            viewModel.loginState.observe(viewLifecycleOwner) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToNoteListFragment())
                } else {
                    Toast.makeText(requireContext(), "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

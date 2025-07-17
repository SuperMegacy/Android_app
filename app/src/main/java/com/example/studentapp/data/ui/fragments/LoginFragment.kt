package com.example.studentapp.data.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.viewmodels.LoginViewModelFactory
import com.example.studentapp.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter




class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<com.example.studentapp.data.ui.viewmodels.LoginViewModel> {
        LoginViewModelFactory(
            repository = getRepository(),
            teacherDao = getDatabase().teacherDao()
        )
    }

    private var loginType: String? = null
    private var selectedTeacher: com.example.studentapp.data.model.Teacher? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        loginType = arguments?.getString("userType")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        when (loginType) {
            "TEACHER" -> setupTeacherLogin()
            "STUDENT" -> setupStudentLogin()
            else -> showInvalidLoginType()
        }
    }

    private fun setupTeacherLogin() {
        binding.apply {
            spinnerTeachers.isVisible = true
            etUsername.isVisible = false
            etPassword.isVisible = false

            lifecycleScope.launch {
                viewModel.teachers.collectLatest { teachers: List<com.example.studentapp.data.model.Teacher> ->
                    if (teachers.isEmpty()) {
                        showNoTeachersError()
                        return@collectLatest
                    }
                    setupTeacherSpinner(teachers)
                }
            }

            btnLogin.setOnClickListener { handleTeacherLogin() }
        }
    }

    private fun setupTeacherSpinner(teachers: List<com.example.studentapp.data.model.Teacher>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            teachers.map { "${it.id} - ${it.firstName} ${it.lastName}" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerTeachers.adapter = adapter
        selectedTeacher = teachers.firstOrNull()

        binding.spinnerTeachers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTeacher = teachers.getOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedTeacher = null
            }
        }
    }

    private fun handleTeacherLogin() {
        selectedTeacher?.let { teacher ->
            saveLoginInfo("TEACHER", teacher.id, teacher.firstName)
            navigateToNoteList()
        } ?: run {
            Toast.makeText(requireContext(), "Please select a teacher", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStudentLogin() {
        binding.apply {
            spinnerTeachers.isVisible = false
            etUsername.isVisible = true
            etPassword.isVisible = true

            btnLogin.setOnClickListener { handleStudentLogin() }
        }

        viewModel.loginState.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                val id = viewModel.studentId.value ?: -1
                val name = viewModel.studentName.value ?: ""
                saveLoginInfo("STUDENT", id, name)
                navigateToNoteList()
            } else if (success == false) {
                showLoginFailed()
            }
        }

    }

    private fun handleStudentLogin() {
        val name = binding.etUsername.text.toString().trim()
        val idText = binding.etPassword.text.toString().trim()

        if (name.isEmpty() || idText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter student name and ID", Toast.LENGTH_SHORT).show()
            return
        }

        val id = idText.toIntOrNull()
        if (id == null) {
            Toast.makeText(requireContext(), "Student ID must be a number", Toast.LENGTH_SHORT)
                .show()
            return
        }

        viewModel.login(id, name)
    }

        private fun showNoTeachersError() {
        Toast.makeText(requireContext(), "No teachers found. Please try again later.", Toast.LENGTH_LONG).show()
        binding.btnLogin.isEnabled = false
    }

    private fun showInvalidLoginType() {
        Toast.makeText(requireContext(), "Invalid login type!", Toast.LENGTH_LONG).show()
        binding.btnLogin.isEnabled = false
    }

    private fun showLoginFailed() {
        Toast.makeText(requireContext(), "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToNoteList() {
        findNavController().navigate(
            LoginFragmentDirections
                .actionLoginFragmentToNoteListFragment()
        )
    }

    @SuppressLint("UseKtx")
    private fun saveLoginInfo(userType: String, userId: Int, name: String) {
        requireContext().getSharedPreferences("StudentAppPrefs", Context.MODE_PRIVATE).edit()
            .putString("userType", userType)
            .putInt("userId", userId)
            .putString("userName", name)
            .apply()
    }

    private fun getRepository(): MainRepository {
        val database = getDatabase()
        return MainRepository.getInstance(
            noteDao = database.noteDao(),
            studentDao = database.studentDao(),
            teacherDao = database.teacherDao()
        )
    }

    private fun getDatabase() = AppDatabase.getInstance(requireContext().applicationContext)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
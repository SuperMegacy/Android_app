package com.example.studentapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.data.model.Student
import com.example.studentapp.databinding.FragmentStudentListBinding
import com.example.studentapp.ui.adapters.StudentAdapter
import com.example.studentapp.data.ui.viewmodels.StudentListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StudentListFragment : Fragment() {

    private var _binding: FragmentStudentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentListViewModel by viewModels()
    private lateinit var adapter: StudentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = StudentAdapter()
        binding.recyclerViewStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewStudents.adapter = adapter

        // Collect Flow in lifecycle-aware coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.students.collectLatest { students ->
                adapter.setStudents(students)
                binding.tvEmptyStudents.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

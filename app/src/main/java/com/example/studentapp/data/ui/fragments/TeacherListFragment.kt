package com.example.studentapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.databinding.FragmentTeacherListBinding
import com.example.studentapp.ui.adapters.TeacherAdapter
import com.example.studentapp.data.ui.viewmodels.TeacherListViewModel

class TeacherListFragment : Fragment() {

    private var _binding: FragmentTeacherListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TeacherListViewModel by viewModels()
    private lateinit var adapter: TeacherAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TeacherAdapter()
        binding.recyclerViewTeachers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTeachers.adapter = adapter

        viewModel.teachers.observe(viewLifecycleOwner) { teachers ->
            adapter.setTeachers(teachers)
            binding.tvEmptyTeachers.visibility = if (teachers.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

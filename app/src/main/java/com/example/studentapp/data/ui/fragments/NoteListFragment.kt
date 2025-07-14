package com.example.studentapp.data.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.databinding.FragmentNoteListBinding
import com.example.studentapp.data.ui.adapters.NoteAdapter
import com.example.studentapp.data.ui.viewmodels.NoteListViewModel
import com.example.studentapp.data.ui.viewmodels.NoteListViewModelFactory

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NoteAdapter
    private val viewModel: NoteListViewModel by viewModels {
        NoteListViewModelFactory(getNoteDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with click handler
        adapter = NoteAdapter { note ->
            // Handle note click here
            // Example: navigateToNoteDetail(note.id)
        }

        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NoteListFragment.adapter
        }

        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            // Use submitList() instead of setNotes()
            adapter.submitList(notes)
            binding.tvEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun getNoteDao() = AppDatabase.getInstance(requireContext().applicationContext).noteDao()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
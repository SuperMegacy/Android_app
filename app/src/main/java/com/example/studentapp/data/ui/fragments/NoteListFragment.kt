package com.example.studentapp.data.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.data.ui.adapters.NoteAdapter
import com.example.studentapp.data.ui.viewmodels.NoteListViewModel
import com.example.studentapp.databinding.FragmentNoteListBinding
import kotlinx.coroutines.launch
import kotlin.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.studentapp.data.local.dao.NoteDao
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.viewmodels.NoteListViewModelFactory
import com.example.studentapp.data.local.AppDatabase


class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val noteListViewModel: NoteListViewModel by lazy {
        val db = AppDatabase.getInstance(requireContext())
        val repository = MainRepository(db.noteDao(), db.studentDao(), db.teacherDao())
        val factory = NoteListViewModelFactory(repository)
        ViewModelProvider(this, factory)[NoteListViewModel::class.java]
    }

    private lateinit var adapter: NoteAdapter

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

        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter { note ->
            navigateToNoteDetails(note.id)
        }

        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NoteListFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddNote.setOnClickListener {
            navigateToNoteDetails(-1)
        }

        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                noteListViewModel.notes.observe(viewLifecycleOwner) { notes ->
                    adapter.submitList(notes)
                    binding.tvEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                }

            }
        }
    }

    private fun navigateToNoteDetails(noteId: Int) {
        val action = NoteListFragmentDirections
            .actionNoteListFragmentToNoteDetailsFragment(noteId)
        findNavController().navigate(action)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    @SuppressLint("UseKtx")
    private fun logout() {
        requireContext()
            .getSharedPreferences("StudentAppPrefs", Context.MODE_PRIVATE)
            .edit()
            .apply()

        val action = NoteListFragmentDirections
            .actionNoteListFragmentToUserSelectionFragment()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

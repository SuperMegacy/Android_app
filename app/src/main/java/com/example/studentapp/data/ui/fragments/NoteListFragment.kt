package com.example.studentapp.data.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.UserType
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.adapters.NoteAdapter
import com.example.studentapp.data.ui.viewmodels.NoteListViewModel
import com.example.studentapp.data.ui.viewmodels.NoteListViewModelFactory
import com.example.studentapp.databinding.FragmentNoteListBinding
import kotlinx.coroutines.launch

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NoteListViewModel
    private lateinit var adapter: NoteAdapter

    private var userId: Int = -1
    private lateinit var userType: UserType
    private lateinit var teachers: List<com.example.studentapp.data.model.Teacher>

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

        arguments?.let {
            val args = NoteListFragmentArgs.fromBundle(it)
            userId = args.userId
            userType = args.userType
        }

        val db = AppDatabase.getInstance(requireContext())
        val repository = MainRepository(db.noteDao(), db.studentDao(), db.teacherDao())
        val factory = NoteListViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NoteListViewModel::class.java]

        // Load teachers once to pass to adapter
        lifecycleScope.launch {
            repository.getAllTeachers().collect { teachersList ->
                teachers = teachersList
                setupRecyclerView()
                setupObservers()
            }
        }


        binding.fabAddNote.visibility = if (userType == UserType.STUDENT) View.VISIBLE else View.GONE
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            userType = userType,
            teachers = teachers,
            onNoteClick = { note -> navigateToNoteDetails(note.id) },
            onNoteDelete = { note -> confirmDeleteNote(note) }
        )

        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
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
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                val notesFlow = when (userType) {
                    UserType.STUDENT -> viewModel.loadStudentNotes(userId)
                    UserType.TEACHER -> viewModel.loadTeacherNotes(userId)
                }

                notesFlow.collect { notes ->
                    adapter.submitList(notes)
                    binding.tvEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun navigateToNoteDetails(noteId: Int) {
        val action = NoteListFragmentDirections
            .actionNoteListFragmentToNoteDetailsFragment(
                noteId = noteId,
                userId = userId,
                userType = userType
            )
        findNavController().navigate(action)
    }

    private fun confirmDeleteNote(note: com.example.studentapp.data.model.Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteNote(note)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                findNavController().navigate(
                    NoteListFragmentDirections.actionNoteListFragmentToUserSelectionFragment()
                )
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

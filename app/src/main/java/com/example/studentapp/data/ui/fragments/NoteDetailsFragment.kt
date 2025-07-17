// NoteDetailsFragment.kt
package com.example.studentapp.data.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModelFactory
import com.example.studentapp.databinding.FragmentNoteDetailsBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class NoteDetailsFragment : Fragment() {

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: NoteDetailsFragmentArgs by navArgs()

    private val viewModel: NoteDetailsViewModel by viewModels {
        NoteDetailsViewModelFactory(
            MainRepository.getInstance(
                AppDatabase.getInstance(requireContext()).noteDao(),
                AppDatabase.getInstance(requireContext()).studentDao(),
                AppDatabase.getInstance(requireContext()).teacherDao()
            )
        )
    }

    private var selectedTeacherId: Int? = null
    private var currentUserId: Int = -1
    private var currentUserType: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("StudentAppPrefs", 0)
        currentUserId = prefs.getInt("userId", -1)
        currentUserType = prefs.getString("userType", "") ?: ""

        if (currentUserType == "STUDENT") {
            setupTeacherSpinner()
        } else {
            binding.spinnerTeacher.isVisible = false
        }

        args.noteId?.let { noteId ->
            viewModel.loadNote(noteId)
        }

        viewModel.note.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.etTitle.setText(it.title)
                binding.etContent.setText(it.description)
                selectedTeacherId = it.teacherId
            }
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(requireContext(), "Title and content are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUserType == "STUDENT" && selectedTeacherId == null) {
                Toast.makeText(requireContext(), "Select a teacher", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val note = Note(
                id = if (args.noteId == -1) 0 else args.noteId, // Only pass 0 for new notes
                title = title,
                description = content,
                studentId = currentUserId,
                teacherId = selectedTeacherId ?: -1,
                imageUrls = emptyList(),
                createdAt = System.currentTimeMillis(),
                marks = null
            )


            MainScope().launch {
                try {
                    viewModel.saveNote(note)
                    Log.d("NoteDetailsFragment", "Note saved successfully: $note")
                    Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e("NoteDetailsFragment", "Error saving note", e)
                    Toast.makeText(requireContext(), "Failed to save note", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun setupTeacherSpinner() {
        viewModel.teachers.observe(viewLifecycleOwner) { teachers ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                teachers.map { "${it.id} - ${it.firstName} ${it.lastName}" }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spinnerTeacher.adapter = adapter
            binding.spinnerTeacher.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedTeacherId = teachers.getOrNull(position)?.id
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedTeacherId = null
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

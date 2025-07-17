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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.studentapp.MainActivity
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModelFactory
import com.example.studentapp.databinding.FragmentNoteDetailsBinding
import kotlinx.coroutines.launch

class NoteDetailsFragment : Fragment() {

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: NoteDetailsFragmentArgs by navArgs()

    private lateinit var viewModel: NoteDetailsViewModel

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

        // Initialize DB, repo, and ViewModel
        val db = AppDatabase.getInstance(requireContext())
        val repository = MainRepository.getInstance(db.noteDao(), db.studentDao(), db.teacherDao())
        viewModel = viewModels<NoteDetailsViewModel> {
            NoteDetailsViewModelFactory(repository)
        }.value

        // Get current user info from SharedPreferences
        val prefs = requireContext().getSharedPreferences("StudentAppPrefs", 0)
        currentUserId = prefs.getInt("userId", -1)
        currentUserType = prefs.getString("userType", "") ?: ""

        // Setup teacher spinner only if current user is a student
        if (currentUserType == "STUDENT") {
            setupTeacherSpinner()
            binding.teacherContainer.isVisible = true
        } else {
            binding.teacherContainer.isVisible = false
        }

        // Load the note for editing or viewing if noteId is valid
        if (args.noteId != -1) {
            viewModel.loadNote(args.noteId)
        } else {
            // New note - enable editing for student
            if (currentUserType == "STUDENT") {
                enableEditing(true)
                binding.btnSave.isVisible = true
                binding.tvMarksDisplay.isVisible = false
                binding.layoutMarks.isVisible = false
            }
        }

        // Observe loaded note and update UI accordingly
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.etTitle.setText(it.title)
                binding.etContent.setText(it.description)
                selectedTeacherId = it.teacherId

                // Update spinner selection if available
                if (currentUserType == "STUDENT" && selectedTeacherId != null) {
                    viewModel.teachers.value?.let { teachers ->
                        val position = teachers.indexOfFirst { teacher -> teacher.id == selectedTeacherId }
                        if (position >= 0) {
                            binding.spinnerTeacher.setSelection(position)
                        }
                    }
                }

                if (currentUserType == "STUDENT") {
                    // Enable editing only if marks is null (not graded yet)
                    val editable = it.marks == null
                    enableEditing(editable)
                    binding.btnSave.isVisible = editable
                    binding.layoutMarks.isVisible = false
                    binding.tvMarksDisplay.isVisible = it.marks != null
                    if (it.marks != null) {
                        binding.tvMarksDisplay.text = "Marks: ${it.marks}"
                    }
                } else if (currentUserType == "TEACHER") {
                    // Teacher cannot edit note fields
                    binding.etTitle.isEnabled = false
                    binding.etContent.isEnabled = false
                    binding.spinnerTeacher.isEnabled = false

                    if (it.marks == null) {
                        // Show marks input and Grade button
                        binding.layoutMarks.isVisible = true
                        binding.btnSave.text = "Grade Note"
                        binding.btnSave.isVisible = true
                        binding.tvMarksDisplay.isVisible = false
                    } else {
                        // Hide marks input and Save button, show marks display
                        binding.layoutMarks.isVisible = false
                        binding.btnSave.isVisible = false
                        binding.tvMarksDisplay.apply {
                            isVisible = true
                            text = "Marks: ${it.marks}"
                        }
                    }
                }
            }
        }

        // Save / Grade button click
        binding.btnSave.setOnClickListener {
            if (currentUserType == "STUDENT") {
                saveStudentNote()
            } else if (currentUserType == "TEACHER") {
                gradeTeacherNote()
            }
        }
    }

    private fun enableEditing(enabled: Boolean) {
        binding.etTitle.isEnabled = enabled
        binding.etContent.isEnabled = enabled
        binding.spinnerTeacher.isEnabled = enabled
    }

    private fun saveStudentNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(requireContext(), "Title and content are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTeacherId == null) {
            Toast.makeText(requireContext(), "Select a teacher", Toast.LENGTH_SHORT).show()
            return
        }

        val note = Note(
            id = if (args.noteId == -1) 0 else args.noteId,
            title = title,
            description = content,
            studentId = currentUserId,
            teacherId = selectedTeacherId ?: -1,
            imageUrls = emptyList(),
            createdAt = System.currentTimeMillis(),
            marks = null
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (args.noteId == -1) {
                    viewModel.createNote(note)
                } else {
                    viewModel.updateNote(note)
                }
                Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Log.e("NoteDetailsFragment", "Error saving note", e)
                Toast.makeText(requireContext(), "Failed to save note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gradeTeacherNote() {
        val markStr = binding.etMarks.text.toString().trim()
        val mark = markStr.toIntOrNull()

        if (mark == null || mark < 0 || mark > 100) {
            Toast.makeText(requireContext(), "Enter a valid mark (0-100)", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.updateNoteMark(args.noteId, mark)
                // Get the updated note
                val updatedNote = viewModel.note.value?.copy(marks = mark)
                updatedNote?.let {
                    // Notify NoteListViewModel of the update
                    (requireActivity() as? MainActivity)?.getNoteListViewModel()?.noteUpdated(it)
                }
                Toast.makeText(requireContext(), "Note graded", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Log.e("NoteDetailsFragment", "Error grading note", e)
                Toast.makeText(requireContext(), "Failed to grade note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTeacherSpinner() {
        viewModel.teachers.observe(viewLifecycleOwner) { teachers ->
            val teacherNames = teachers.map { "${it.firstName} ${it.lastName}" }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teacherNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTeacher.adapter = adapter

            // Set spinner selection if we have a selected teacher
            selectedTeacherId?.let { teacherId ->
                val position = teachers.indexOfFirst { it.id == teacherId }
                if (position >= 0) {
                    binding.spinnerTeacher.setSelection(position)
                }
            }

            binding.spinnerTeacher.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedTeacherId = teachers[position].id
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
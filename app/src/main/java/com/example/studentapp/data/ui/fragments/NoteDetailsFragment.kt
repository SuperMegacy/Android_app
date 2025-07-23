package com.example.studentapp.data.ui.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.R
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.model.UserType
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.adapters.NoteImageAdapter
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel
import com.example.studentapp.databinding.FragmentNoteDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import java.util.Locale

class NoteDetailsFragment : Fragment() {

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: NoteDetailsFragmentArgs by navArgs()
    private lateinit var viewModel: NoteDetailsViewModel
    private lateinit var imageAdapter: NoteImageAdapter
    private var selectedTeacherId: Int? = null

    // Permissions for image capture (only relevant for students)
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) showImagePickerDialog()
        else showToast(R.string.permissions_required)
    }

    private val cameraLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                viewModel.tempImageUri?.let { viewModel.saveImageToPermanentStorage(it) }
            } else {
                showToast(R.string.camera_failed)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveImageToPermanentStorage(it) }
            ?: showToast(R.string.no_image_selected)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContext = requireContext().applicationContext
        val db = AppDatabase.getInstance(appContext)
        val repository = MainRepository.getInstance(
            noteDao = db.noteDao(),
            studentDao = db.studentDao(),
            teacherDao = db.teacherDao()
        )

        val userType = args.userType

        val factory = NoteDetailsViewModel.provideFactory(
            application = requireActivity().application,
            noteId = args.noteId,
            userId = args.userId,
            userType = userType,
            repository = repository
        )

        viewModel = ViewModelProvider(this, factory)[NoteDetailsViewModel::class.java]
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup UI components
        setupImageRecyclerView()
        setupViewListeners()
        observeViewModel()

        // Collect UI and note state changes
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(viewModel.uiState, viewModel.noteState) { uiState, noteState ->
                Pair(uiState, noteState)
            }.collect { (uiState, noteState) ->
                binding.imageCaptureRow.isVisible = uiState.showImageAttachmentOptions
                binding.teacherContainer.isVisible = uiState.showTeacherSpinner
                binding.layoutMarks.isVisible = uiState.showMarksInput
                binding.btnSubmitMarks.isVisible = uiState.showMarksInput
                binding.btnSave.isVisible = uiState.showSaveButton
                binding.btnSave.text = getString(uiState.saveButtonTextResId)

                binding.etTitle.isEnabled = uiState.isEditable
                binding.etContent.isEnabled = uiState.isEditable

                updateUI(noteState, uiState)
            }
        }


        // Collect UI events
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is NoteDetailsViewModel.UIEvent.ShowMessage -> showToast(event.message)
                    is NoteDetailsViewModel.UIEvent.ShowValidationError -> showToast(event.message)
                    is NoteDetailsViewModel.UIEvent.NavigateBack -> requireActivity().onBackPressed()
                }
            }
        }

        loadInitialData()
    }

    private fun setupImageRecyclerView() {
        imageAdapter = NoteImageAdapter(
            context = requireContext(),
            onImageRemoved = { position -> viewModel.removeImageAtIndex(position) }
        )
        binding.recyclerViewImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupViewListeners() {
        if (viewModel.isStudent()) {
            binding.btnAddImage.setOnClickListener { checkPermissionsAndShowPicker() }
        } else {
            binding.btnAddImage.isVisible = false
        }

        binding.btnSave.setOnClickListener {
            if (viewModel.isStudent()) {
                saveStudentNote()
            }
        }

        binding.btnSubmitMarks.setOnClickListener {
            if (!viewModel.isStudent()) {
                gradeTeacherNote()
            }
        }
    }

    private fun observeViewModel() {
        // Observe teachers list (only relevant for students)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.teachers.collect { teachers ->
                if (viewModel.isStudent()) {
                    teachers?.let { setupTeacherSpinner(it) }
                }
            }
        }

        // Observe note state changes (handled in combine flow above)
    }

    private fun updateUI(state: NoteDetailsViewModel.NoteState, uiState: NoteDetailsViewModel.UiState) = with(binding) {
        state.note?.let { note ->
            etTitle.setText(note.title)
            etContent.setText(note.description)
            selectedTeacherId = note.teacherId
            if (viewModel.isStudent()) {
                val editable = note.marks == null
                enableEditing(editable)
                btnSave.isVisible = uiState.showSaveButton && editable
                layoutMarks.isVisible = false
                btnSubmitMarks.isVisible = false
                tvMarksDisplay.isVisible = note.marks != null
                tvMarksDisplay.text = note.marks?.let { getString(R.string.marks_format, it) }
            } else {
                enableEditing(false)
                layoutMarks.isVisible = uiState.showMarksInput
                btnSubmitMarks.isVisible = uiState.showMarksInput
                btnSave.isVisible = uiState.showSaveButton
                tvMarksDisplay.isVisible = note.marks != null
                tvMarksDisplay.text = note.marks?.let { getString(R.string.marks_format, it) }
                note.marks?.let { etMarks.setText(it.toString()) }
            }
        }
        imageAdapter.submitList(state.imageUris)
    }

    private fun loadInitialData() {
        if (args.noteId != -1) {
            viewModel.loadNote(args.noteId)
        } else if (viewModel.isStudent()) {
            enableEditing(true)
            binding.btnSave.isVisible = true
            binding.btnSubmitMarks.isVisible = false
        } else {
            binding.btnSave.isVisible = false
            binding.btnSubmitMarks.isVisible = true
        }
    }

    private fun setupTeacherSpinner(teachers: List<Teacher>) {
        val teacherNames = teachers.map { "${it.firstName} ${it.lastName}" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            teacherNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerTeacher.adapter = adapter

        val selectedIndex = teachers.indexOfFirst { it.id == selectedTeacherId }
        if (selectedIndex != -1) binding.spinnerTeacher.setSelection(selectedIndex)

        binding.spinnerTeacher.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTeacherId = teachers[position].id
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                selectedTeacherId = null
            }
        }
    }

    private fun checkPermissionsAndShowPicker() {
        if (!viewModel.isStudent()) return

        when {
            hasPermissions() -> showImagePickerDialog()
            shouldShowRequestPermissionRationale() -> showPermissionRationale()
            else -> permissionLauncher.launch(permissions)
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
        }
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_needed)
            .setMessage(R.string.image_picker_permission_rationale)
            .setPositiveButton(R.string.grant) { _, _ -> permissionLauncher.launch(permissions) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showImagePickerDialog() {
        if (!viewModel.isStudent()) return

        if (viewModel.hasReachedImageLimit()) {
            showToast(R.string.max_images_reached)
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_image)
            .setItems(R.array.image_picker_options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> launchGallery()
                    else -> Log.w(TAG, "Unknown image picker option selected")
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun launchCamera() {
        try {
            val tempFile = viewModel.createTempImageFile()
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                tempFile
            )
            viewModel.setTempImageUri(uri)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Camera launch failed", e)
            showToast(R.string.error_launching_camera)
        }
    }

    private fun launchGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.error_no_gallery_app)
        }
    }

    private fun saveStudentNote() = with(binding) {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            showToast(R.string.title_content_required)
            return
        }

        if (selectedTeacherId == null) {
            showToast(R.string.select_teacher)
            return
        }

        viewModel.saveNote(title, content, selectedTeacherId!!)
    }

    private fun gradeTeacherNote() {
        val markStr = binding.etMarks.text.toString().trim()
        val mark = markStr.toIntOrNull()
        if (mark == null || mark !in 0..100) {
            showToast(R.string.invalid_marks)
            return
        }

        viewModel.gradeNote(mark)
    }

    private fun enableEditing(enabled: Boolean) = with(binding) {
        val actuallyEnabled = enabled && viewModel.isStudent()
        etTitle.isEnabled = actuallyEnabled
        etContent.isEnabled = actuallyEnabled
        spinnerTeacher.isEnabled = actuallyEnabled
        btnAddImage.isVisible = actuallyEnabled
    }

    private fun showToast(@StringRes messageResId: Int) {
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "NoteDetailsFragment"
    }
}
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.R
import com.example.studentapp.data.ui.adapters.NoteImageAdapter
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel.NoteState
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel.UIEvent
import com.example.studentapp.databinding.FragmentNoteDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.observe
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File


@AndroidEntryPoint
class NoteDetailsFragment : Fragment() {

    // View binding instance (nullable to avoid memory leaks)
    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    // Navigation arguments using Safe Args
    private val args: NoteDetailsFragmentArgs by navArgs()

    // ViewModel instance injected by Hilt
    private val viewModel: NoteDetailsViewModel by viewModels()

    // Adapter for displaying note images
    private lateinit var imageAdapter: NoteImageAdapter

    // Currently selected teacher ID (view state, not business logic)
    private var selectedTeacherId: Int? = null

    // Permission handling based on Android version
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    // Permission launcher for runtime permission requests
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            showImagePickerDialog()
        } else {
            showToast(R.string.permissions_required)
        }
    }

    // Camera launcher for capturing images
    private val cameraLauncher: ActivityResultLauncher<Uri?>
        get() = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                viewModel.tempImageUri?.let { viewModel.saveImageToPermanentStorage(it) }
            } else {
                showToast(R.string.camera_failed)
            }
        }

    // Gallery launcher for selecting images
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveImageToPermanentStorage(it) } ?: showToast(R.string.no_image_selected)
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

        setupImageRecyclerView()  // Initialize image adapter
        setupViewListeners()      // Setup click listeners
        observeViewModel()        // Observe ViewModel state
        loadInitialData()         // Load note or setup new note
    }

    /**
     * Initializes the RecyclerView and image adapter
     * Note: Pure UI setup, no business logic here
     */
    private fun setupImageRecyclerView() {
        imageAdapter = NoteImageAdapter(
            context = requireContext(),
            onImageRemoved = { position ->
                // Delegate image removal to ViewModel
                viewModel.removeImageAtIndex(position)
            }
        )

        binding.recyclerViewImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    /**
     * Sets up all view click listeners
     * Note: Only handles UI interactions, delegates logic to ViewModel
     */
    private fun setupViewListeners() {
        binding.btnAddImage.setOnClickListener {
            checkPermissionsAndShowPicker()
        }

        binding.btnSave.setOnClickListener {
            if (viewModel.isStudent()) {
                saveStudentNote()
            } else {
                gradeTeacherNote()
            }
        }
    }

    /**
     * Observes ViewModel state and events
     * Maintains strict separation between View and ViewModel
     */
    private fun observeViewModel() {
        // Observe note state changes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteState.collectLatest { state ->
                updateUI(state)
                // Update images through adapter (UI concern)
                imageAdapter.submitList(state.imageUris)
            }
        }

        // Observe one-time UI events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.teachers.collect { teachers ->
                    setupTeacherSpinner(teachers)
                }
            }
        }


    }

    /**
     * Loads initial data based on navigation arguments
     */
    private fun loadInitialData() {
        if (args.noteId != -1) {
            // Existing note - load from ViewModel
            viewModel.loadNote(args.noteId)
        } else if (viewModel.isStudent()) {
            // New note - enable editing for students
            enableEditing(true)
            binding.btnSave.isVisible = true
        }
    }

    /**
     * Updates UI based on ViewModel state
     * @param state The current NoteState from ViewModel
     */
    private fun updateUI(state: NoteState) = with(binding) {
        state.note?.let { note ->
            // Update text fields
            etTitle.setText(note.title)
            etContent.setText(note.description)
            selectedTeacherId = note.teacherId

            // Configure UI based on user type and note state
            if (viewModel.isStudent()) {
                val editable = note.marks == null
                enableEditing(editable)
                btnSave.isVisible = editable
                layoutMarks.isVisible = false
                tvMarksDisplay.isVisible = note.marks != null
                tvMarksDisplay.text = note.marks?.let { getString(R.string.marks_format, it) }
            } else {
                enableEditing(false)
                layoutMarks.isVisible = note.marks == null
                btnSave.isVisible = note.marks == null
                tvMarksDisplay.isVisible = note.marks != null
                tvMarksDisplay.text = note.marks?.let { getString(R.string.marks_format, it) }
                btnSave.text = getString(R.string.grade_note)
            }
        }
    }

    /**
     * Sets up teacher spinner with data from ViewModel
     * @param teachers List of teachers from ViewModel
     */
    private fun setupTeacherSpinner(teachers: List<com.example.studentapp.data.model.Teacher>?) {
        if (teachers == null) return

        val teacherNames = teachers.map { "${it.firstName} ${it.lastName}" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            teacherNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerTeacher.adapter = adapter

        // Set selected teacher if available
        val selectedIndex = teachers.indexOfFirst { it.id == selectedTeacherId }
        if (selectedIndex != -1) binding.spinnerTeacher.setSelection(selectedIndex)

        // Handle selection changes
        binding.spinnerTeacher.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>,
                                        view: View?, position: Int, id: Long) {
                selectedTeacherId = teachers[position].id
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                selectedTeacherId = null
            }
        }

        // Only show teacher spinner for students
        binding.teacherContainer.isVisible = viewModel.isStudent()
    }

    /* -------------------------
       Image Handling Methods
       ------------------------- */

    /* -------------------------
       Image Handling Methods
       ------------------------- */

    private fun checkPermissionsAndShowPicker() {
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
            .setPositiveButton(R.string.grant) { _, _ ->
                permissionLauncher.launch(permissions)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showImagePickerDialog() {
        if (viewModel.hasReachedImageLimit()) {
            showMaxImagesReached()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_image)
            .setItems(R.array.image_picker_options) { _, which ->
                handleImagePickerSelection(which)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleImagePickerSelection(which: Int) {
        when (which) {
            0 -> launchCamera()
            1 -> launchGallery()
            else -> Log.w(TAG, "Unknown image picker option selected")
        }
    }

    private fun launchCamera() {
        try {
            val tempFile = viewModel.createTempImageFile()

            val uri = createFileProviderUri(tempFile)
            viewModel.setTempImageUri(uri)
            launchCameraIntent(uri)
        } catch (e: Exception) {
            handleCameraError(e)
        }
    }

    private fun createFileProviderUri(file: File): Uri {
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
    }

    private fun launchCameraIntent(uri: Uri) {
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.error_no_camera_app)
        } catch (e: SecurityException) {
            showToast(R.string.error_camera_permission)
        }
    }

    private fun launchGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.error_no_gallery_app)
        }
    }

    private fun showMaxImagesReached() {
        showToast(R.string.max_images_reached)
    }

    private fun showFileCreationError() {
        showToast(R.string.error_creating_file)
    }

    private fun handleCameraError(e: Exception) {
        Log.e(TAG, "Camera launch failed", e)
        showToast(R.string.error_launching_camera)
    }

    companion object {
        private const val TAG = "NoteDetailsFragment"
    }
    /* -------------------------
       Note Saving Methods
       ------------------------- */

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

    /* -------------------------
       Utility Methods
       ------------------------- */

    private fun enableEditing(enabled: Boolean) = with(binding) {
        etTitle.isEnabled = enabled
        etContent.isEnabled = enabled
        spinnerTeacher.isEnabled = enabled
        btnAddImage.isVisible = enabled
    }

    private fun showToast(@StringRes messageResId: Int) {
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Prevent memory leaks
    }
}
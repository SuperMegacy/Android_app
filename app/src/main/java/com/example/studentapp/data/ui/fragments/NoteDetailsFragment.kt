package com.example.studentapp.data.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.studentapp.R
import com.example.studentapp.data.local.AppDatabase
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModel
import com.example.studentapp.data.ui.viewmodels.NoteDetailsViewModelFactory
import com.example.studentapp.databinding.FragmentNoteDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailsFragment : Fragment() {

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: NoteDetailsFragmentArgs by navArgs()
    private lateinit var viewModel: NoteDetailsViewModel

    private var selectedTeacherId: Int? = null
    private var currentUserId: Int = -1
    private var currentUserType: String = ""

    private val selectedImageUris = mutableListOf<Uri>()
    private var tempImageUri: Uri? = null

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            showImagePickerDialog()
        } else {
            Toast.makeText(
                requireContext(),
                "Permissions required to add images",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleImageSave(uri: Uri) {
        lifecycleScope.launch {
            try {
                val permanentUri = saveImageToPermanentStorage(uri)
                permanentUri?.let {
                    selectedImageUris.add(it)
                    renderImagePreviews()
                    showToast("Image saved successfully")
                } ?: showToast("Failed to save image: null URI returned")
            } catch (e: Exception) {
                showToast("Error saving image: ${e.localizedMessage}")
                Log.e("ImageSave", "Error saving image", e)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                handleImageSave(uri)
            } ?: showToast("Error: Temporary image URI is null")
        } else {
            showToast("Camera operation failed or was cancelled")
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            handleImageSave(uri)
        } else {
            showToast("No image selected from gallery")
        }
    }

    // Helper function to show toast messages
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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

        setupViewModel()
        setupUserInfo()
        setupViews()
        setupObservers()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getInstance(requireContext())
        val repository = MainRepository.getInstance(db.noteDao(), db.studentDao(), db.teacherDao())
        viewModel = viewModels<NoteDetailsViewModel> {
            NoteDetailsViewModelFactory(repository)
        }.value
    }

    private fun setupUserInfo() {
        val prefs = requireContext().getSharedPreferences("StudentAppPrefs", 0)
        currentUserId = prefs.getInt("userId", -1)
        currentUserType = prefs.getString("userType", "") ?: ""
    }

    private fun setupViews() {
        binding.btnAddImage.setOnClickListener {
            if (hasPermissions()) {
                showImagePickerDialog()
            } else {
                permissionLauncher.launch(permissions)
            }
        }

        binding.btnSave.setOnClickListener {
            if (currentUserType == "STUDENT") {
                saveStudentNote()
            } else {
                gradeTeacherNote()
            }
        }

        if (currentUserType == "STUDENT") {
            setupTeacherSpinner()
            binding.teacherContainer.isVisible = true
        } else {
            binding.teacherContainer.isVisible = false
        }

        if (args.noteId != -1) {
            viewModel.loadNote(args.noteId)
        } else if (currentUserType == "STUDENT") {
            enableEditing(true)
            binding.btnSave.isVisible = true
            binding.tvMarksDisplay.isVisible = false
            binding.layoutMarks.isVisible = false
        }
    }

    private fun setupObservers() {
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.etTitle.setText(it.title)
                binding.etContent.setText(it.description)
                selectedTeacherId = it.teacherId

                selectedImageUris.clear()
                it.imageUrls.mapNotNull { uriString ->
                    runCatching { Uri.parse(uriString) }.getOrNull()
                }.let { uris ->
                    selectedImageUris.addAll(uris)
                    renderImagePreviews()
                }

                if (currentUserType == "STUDENT") {
                    val editable = it.marks == null
                    enableEditing(editable)
                    binding.btnSave.isVisible = editable
                    binding.layoutMarks.isVisible = false
                    binding.tvMarksDisplay.isVisible = it.marks != null
                    if (it.marks != null) {
                        binding.tvMarksDisplay.text = getString(R.string.marks_format, it.marks)
                    }
                } else {
                    enableEditing(false)
                    if (it.marks == null) {
                        binding.layoutMarks.isVisible = true
                        binding.btnSave.text = getString(R.string.grade_note)
                        binding.btnSave.isVisible = true
                    } else {
                        binding.layoutMarks.isVisible = false
                        binding.btnSave.isVisible = false
                        binding.tvMarksDisplay.isVisible = true
                        binding.tvMarksDisplay.text = getString(R.string.marks_format, it.marks)
                    }
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun createTempImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().cacheDir
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                createNewFile()
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun showImagePickerDialog() {
        if (selectedImageUris.size >= 2) {
            Toast.makeText(
                requireContext(),
                getString(R.string.max_images_reached),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_image))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> launchGallery()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val photoFile = createTempImageFile() ?: run {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_creating_file),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )

        cameraLauncher.launch(tempImageUri)
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    suspend fun saveImageToPermanentStorage(uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                val filename = "note_image_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StudentApp")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val permanentUri = requireContext().contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                permanentUri?.let {
                    requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        requireContext().contentResolver.update(it, contentValues, null, null)
                    }

                    it
                }
            } catch (e: Exception) {
                Log.e("NoteDetailsFragment", "Error saving image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_saving_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }
        }
    }

    private fun renderImagePreviews() {
        binding.imagePreviewContainer.removeAllViews()

        selectedImageUris.forEachIndexed { index, uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.image_preview_size),
                    resources.getDimensionPixelSize(R.dimen.image_preview_size)
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.image_preview_margin)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = ContextCompat.getDrawable(requireContext(), R.drawable.image_preview_bg)
            }

            val closeButton = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.image_delete_button_size),
                    resources.getDimensionPixelSize(R.dimen.image_delete_button_size)
                ).apply {
                    gravity = Gravity.END or Gravity.TOP
                }
                setImageResource(R.drawable.ic_close)
                setOnClickListener {
                    selectedImageUris.removeAt(index)
                    renderImagePreviews()
                }
            }

            val container = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(imageView)
                addView(closeButton)
            }

            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imageView)

            binding.imagePreviewContainer.addView(container)
        }
    }

    private fun enableEditing(enabled: Boolean) {
        binding.etTitle.isEnabled = enabled
        binding.etContent.isEnabled = enabled
        binding.spinnerTeacher.isEnabled = enabled
        binding.btnAddImage.isVisible = enabled
    }

    private fun setupTeacherSpinner() {
        viewModel.teachers.observe(viewLifecycleOwner) { teachers ->
            val teacherNames = teachers.map { "${it.firstName} ${it.lastName}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                teacherNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spinnerTeacher.adapter = adapter
            selectedTeacherId?.let { teacherId ->
                teachers.indexOfFirst { it.id == teacherId }.takeIf { it >= 0 }?.let { position ->
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

    private fun saveStudentNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.title_content_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (selectedTeacherId == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.select_teacher),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val note = Note(
            id = if (args.noteId == -1) 0 else args.noteId,
            title = title,
            description = content,
            studentId = currentUserId,
            teacherId = selectedTeacherId ?: -1,
            imageUrls = selectedImageUris.map { it.toString() },
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
                Toast.makeText(
                    requireContext(),
                    getString(R.string.note_saved),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Log.e("NoteDetailsFragment", "Error saving note", e)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_saving_note),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun gradeTeacherNote() {
        val markStr = binding.etMarks.text.toString().trim()
        val mark = markStr.toIntOrNull()

        if (mark == null || mark !in 0..100) {
            Toast.makeText(
                requireContext(),
                getString(R.string.invalid_marks),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.updateNoteMark(args.noteId, mark)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.note_graded),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Log.e("NoteDetailsFragment", "Error grading note", e)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_grading_note),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
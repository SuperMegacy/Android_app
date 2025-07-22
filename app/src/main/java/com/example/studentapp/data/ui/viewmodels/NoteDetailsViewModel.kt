package com.example.studentapp.data.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.studentapp.R
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.model.UserType
import com.example.studentapp.data.repository.MainRepository
import com.example.studentapp.data.utils.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class NoteDetailsViewModel(
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    application: Application,
    private val noteId: Int = -1
) : AndroidViewModel(application) {

    companion object {
        private const val MAX_IMAGES = 4
        private const val MIN_MARKS = 0
        private const val MAX_MARKS = 100

        fun provideFactory(
            application: Application,
            noteId: Int,
            repository: MainRepository,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NoteDetailsViewModel(
                        repository,
                        sessionManager,
                        application,
                        noteId
                    ) as T
                }
            }
        }
    }

    data class NoteState(
        val note: Note? = null,
        val imageUris: List<Uri> = emptyList(),
        val isLoading: Boolean = false
    )

    data class UiState(
        val showImageAttachmentOptions: Boolean = true,
        val showTeacherSpinner: Boolean = true,
        val showMarksInput: Boolean = false,
        val showSaveButton: Boolean = true, // Added to control save button visibility
        val saveButtonTextResId: Int = R.string.save_note,
        val isEditable: Boolean = true
    )

    sealed class UIEvent {
        data class ShowMessage(val message: Int) : UIEvent()
        data class ShowValidationError(val message: Int) : UIEvent()
        object NavigateBack : UIEvent()
    }

    private val _noteState = MutableStateFlow(NoteState())
    val noteState: StateFlow<NoteState> = _noteState.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UIEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var _tempImageUri: Uri? = null
    val tempImageUri get() = _tempImageUri

    val teachers: Flow<List<Teacher>> = repository.getAllTeachers()
        .take(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        initializeUiState()
        if (noteId != -1) {
            loadNote(noteId)
        }
    }

    private fun initializeUiState() {
        val isTeacher = !isStudent()
        _uiState.value = UiState(
            showImageAttachmentOptions = !isTeacher,
            showTeacherSpinner = !isTeacher,
            showMarksInput = isTeacher,
            showSaveButton = true,
            saveButtonTextResId = if (isTeacher) R.string.add_update_marks else R.string.save_note,
            isEditable = if (isTeacher) false else noteId == -1
        )
    }


    fun loadNote(noteId: Int) {
        _noteState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                repository.getNoteById(noteId)?.let { note ->
                    _noteState.update {
                        it.copy(
                            note = note,
                            imageUris = note.imageUrls.mapNotNull(Uri::parse),
                            isLoading = false
                        )
                    }
                    updateUiStateForLoadedNote(note)
                } ?: run {
                    _noteState.update { it.copy(isLoading = false) }
                    _uiEvents.send(UIEvent.ShowMessage(R.string.error_note_not_found))
                }
            } catch (e: Exception) {
                _noteState.update { it.copy(isLoading = false) }
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_loading_note))
            }
        }
    }

    private fun updateUiStateForLoadedNote(note: Note) {
        val isTeacher = !isStudent()
        val canEdit = if (isTeacher) {
            note.marks == null
        } else {
            note.studentId == getCurrentUserId() && note.marks == null
        }

        _uiState.update {
            it.copy(
                showMarksInput = isTeacher && note.marks == null,
                showSaveButton = !isTeacher, // Hide save button for teachers
                isEditable = canEdit,
                saveButtonTextResId = if (isTeacher) R.string.add_update_marks else R.string.save_note

            )
        }
    }

    fun saveNote(title: String, description: String, teacherId: Int) {
        if (title.isBlank() || description.isBlank()) {
            viewModelScope.launch {
                _uiEvents.send(UIEvent.ShowValidationError(R.string.title_content_required))
            }
            return
        }

        _noteState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val currentNote = _noteState.value.note
                val imageUrls = _noteState.value.imageUris.map { it.toString() }

                val noteToSave = currentNote?.copy(
                    title = title,
                    description = description,
                    teacherId = teacherId,
                    imageUrls = imageUrls
                ) ?: Note(
                    id = 0,
                    title = title,
                    description = description,
                    teacherId = teacherId,
                    studentId = getCurrentUserId(),
                    marks = null,
                    imageUrls = imageUrls,
                    createdAt = System.currentTimeMillis()
                )

                if (currentNote == null) {
                    repository.insertNote(noteToSave)
                } else {
                    repository.updateNote(noteToSave)
                }

                _noteState.update { it.copy(note = noteToSave, isLoading = false) }
                _uiEvents.send(UIEvent.ShowMessage(R.string.note_saved))
                _uiEvents.send(UIEvent.NavigateBack)
            } catch (e: Exception) {
                _noteState.update { it.copy(isLoading = false) }
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_saving_note))
            }
        }
    }

    fun gradeNote(mark: Int) {
        if (isStudent()) {
            viewModelScope.launch {
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_unauthorized_action))
            }
            return
        }
        if (mark !in MIN_MARKS..MAX_MARKS) {
            viewModelScope.launch {
                _uiEvents.send(UIEvent.ShowValidationError(R.string.invalid_marks))
            }
            return
        }

        _noteState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val currentNote = _noteState.value.note ?: run {
                    _noteState.update { it.copy(isLoading = false) }
                    _uiEvents.send(UIEvent.ShowMessage(R.string.error_note_not_loaded))
                    return@launch
                }

                val gradedNote = currentNote.copy(marks = mark)
                repository.updateNote(gradedNote)

                _noteState.update {
                    it.copy(
                        note = gradedNote,
                        isLoading = false
                    )
                }
                _uiEvents.send(UIEvent.ShowMessage(R.string.note_graded))
                _uiEvents.send(UIEvent.NavigateBack)
            } catch (e: Exception) {
                _noteState.update { it.copy(isLoading = false) }
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_grading_note))
            }
        }
    }

    fun removeImageAtIndex(index: Int) {
        if (!isStudent()) return

        val currentUris = _noteState.value.imageUris.toMutableList()
        if (index in currentUris.indices) {
            currentUris.removeAt(index)
            _noteState.update { it.copy(imageUris = currentUris) }
        }
    }

    fun saveImageToPermanentStorage(uri: Uri) {
        if (!isStudent() || _noteState.value.imageUris.size >= MAX_IMAGES) {
            viewModelScope.launch {
                _uiEvents.send(UIEvent.ShowMessage(R.string.max_images_reached))
            }
            return
        }
        _noteState.update {
            it.copy(imageUris = it.imageUris + uri)
        }
    }

    fun hasReachedImageLimit(): Boolean =
        !isStudent() || _noteState.value.imageUris.size >= MAX_IMAGES

    fun setTempImageUri(uri: Uri) {
        _tempImageUri = uri
    }

    fun createTempImageFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "IMG_$timestamp"
        val storageDir = File(getApplication<Application>().cacheDir, "images").apply { mkdirs() }
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    fun isStudent(): Boolean {
        val userType = sessionManager.getCurrentUserType()
        return userType == UserType.STUDENT
    }

    private fun getCurrentUserId(): Int = sessionManager.getCurrentUserId()
}
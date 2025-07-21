package com.example.studentapp.data.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapp.R
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.data.model.UserType
import com.example.studentapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import com.example.studentapp.data.utils.SessionManager

@HiltViewModel
class NoteDetailsViewModel @Inject constructor(
    private val repository: MainRepository,
    private val sessionManager: SessionManager,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val MAX_IMAGES = 4
    }

    data class NoteState(
        val note: Note? = null,
        val imageUris: List<Uri> = emptyList()
    )

    sealed class UIEvent {
        data class ShowMessage(val message: Int) : UIEvent()
        object NavigateBack : UIEvent()
    }

    private val _noteState = MutableStateFlow(NoteState())
    val noteState: StateFlow<NoteState> = _noteState.asStateFlow()

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

    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            repository.getNoteById(noteId)?.let { note ->
                _noteState.update {
                    it.copy(
                        note = note,
                        imageUris = note.imageUrls.map(Uri::parse)
                    )
                }
            }
        }
    }

    fun saveNote(title: String, description: String, teacherId: Int) {
        viewModelScope.launch {
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

            try {
                if (currentNote == null) {
                    repository.insertNote(noteToSave)
                } else {
                    repository.updateNote(noteToSave)
                }
                _noteState.update { it.copy(note = noteToSave) }
                _uiEvents.send(UIEvent.ShowMessage(R.string.note_saved))
                _uiEvents.send(UIEvent.NavigateBack)
            } catch (e: Exception) {
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_saving_note))
            }
        }
    }

    fun gradeNote(mark: Int) {
        viewModelScope.launch {
            val currentNote = _noteState.value.note ?: run {
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_note_not_loaded))
                return@launch
            }

            val gradedNote = currentNote.copy(marks = mark)
            try {
                repository.updateNote(gradedNote)
                _noteState.update { it.copy(note = gradedNote) }
                _uiEvents.send(UIEvent.ShowMessage(R.string.note_graded))
                _uiEvents.send(UIEvent.NavigateBack)
            } catch (e: Exception) {
                _uiEvents.send(UIEvent.ShowMessage(R.string.error_grading_note))
            }
        }
    }

    fun removeImageAtIndex(index: Int) {
        val currentUris = _noteState.value.imageUris.toMutableList()
        if (index in currentUris.indices) {
            currentUris.removeAt(index)
            _noteState.update { it.copy(imageUris = currentUris) }
        }
    }

    fun saveImageToPermanentStorage(uri: Uri) {
        if (_noteState.value.imageUris.size >= MAX_IMAGES) {
            viewModelScope.launch {
                _uiEvents.send(UIEvent.ShowMessage(R.string.max_images_reached))
            }
            return
        }
        _noteState.update {
            it.copy(imageUris = it.imageUris + uri)
        }
    }

    fun hasReachedImageLimit(): Boolean = _noteState.value.imageUris.size >= MAX_IMAGES

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
        val userType = sessionManager.getCurrentUserType(getApplication())
        return userType == UserType.STUDENT
    }

    private fun getCurrentUserId(): Int = sessionManager.getCurrentUserId(getApplication())
}

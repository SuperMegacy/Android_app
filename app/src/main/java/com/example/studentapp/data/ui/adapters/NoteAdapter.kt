package com.example.studentapp.data.ui.adapters

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapp.data.model.Note
import com.example.studentapp.data.model.UserType
import com.example.studentapp.databinding.ItemNoteBinding

class NoteAdapter(
    private val userType: UserType,
    private val teachers: List<com.example.studentapp.data.model.Teacher>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDelete: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DiffCallback()) {

    fun updateNote(updatedNote: Note) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedNote.id }
        if (index != -1) {
            currentList[index] = updatedNote
            submitList(currentList)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding, onNoteClick, onNoteDelete, userType, teachers)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<Note>?) {
        super.submitList(list?.toList())
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onNoteClick: (Note) -> Unit,
        private val onNoteDelete: (Note) -> Unit,
        private val userType: UserType,
        private val teachers: List<com.example.studentapp.data.model.Teacher>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvNoteTitle.text = note.title
            tvNoteMarks.text = note.marks?.let { "Grade: $it" } ?: "Not graded"
            tvNoteDate.text = note.createdAt?.let {
                DateFormat.format("MMM dd, yyyy", it).toString()
            } ?: "No date"

            // Find teacher name by teacherId
            val teacherName = teachers.find { it.id == note.teacherId }
                ?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown Teacher"
            tvTeacherName.text = "Teacher: $teacherName"

            // Show delete icon only if logged in user is STUDENT
            ivDeleteNote.visibility = if (userType == UserType.STUDENT) View.VISIBLE else View.GONE

            ivDeleteNote.setOnClickListener {
                onNoteDelete(note)
            }

            root.setOnClickListener {
                onNoteClick(note)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.id == newItem.id

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.title == newItem.title &&
                    oldItem.description == newItem.description &&
                    oldItem.marks == newItem.marks &&
                    oldItem.teacherId == newItem.teacherId &&
                    oldItem.studentId == newItem.studentId &&
                    oldItem.imageUrls == newItem.imageUrls
    }
}

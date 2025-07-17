package com.example.studentapp.data.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapp.data.model.Note
import com.example.studentapp.databinding.ItemNoteBinding

class NoteAdapter(
    private val onNoteClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DiffCallback()) {

    // update single item in the list
    fun updateNote(updatedNote: Note) {
        val currentList = currentList.toMutableList()
        val position = currentList.indexOfFirst { it.id == updatedNote.id }
        if (position != -1) {
            currentList[position] = updatedNote
            submitList(currentList)
        }
    }

    companion object {
        private const val TAG = "NoteAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        Log.d(TAG, "Creating new view holder")
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding, onNoteClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        Log.d(TAG, "Binding note at position $position: ${note.id}")
        holder.bind(note)
    }

    override fun submitList(list: List<Note>?) {
        Log.d(TAG, "Submitting new list with ${list?.size ?: 0} items")
        super.submitList(list?.let { ArrayList(it) }) // Create new list to ensure diffing works
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onNoteClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvTitle.text = note.title
            tvGrade.text = note.marks?.let { "Grade: $it" } ?: "Not graded"
            tvDate.text = note.createdAt?.let {
                android.text.format.DateFormat.format("MMM dd, yyyy", it)
            } ?: "No date"

            root.setOnClickListener {
                Log.d(TAG, "Note clicked: ${note.id} - ${note.title}")
                onNoteClick(note)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.description == newItem.description &&
                    oldItem.marks == newItem.marks &&
                    oldItem.teacherId == newItem.teacherId &&
                    oldItem.studentId == newItem.studentId
        }
    }
}
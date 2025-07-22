package com.example.studentapp.data.ui.adapters

import android.annotation.SuppressLint
import android.text.format.DateFormat
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

    /**
     * Updates a single Note item in the list, maintaining immutability for DiffUtil.
     */
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
        return NoteViewHolder(binding, onNoteClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))

    }

    override fun submitList(list: List<Note>?) {
        // Defensive copy to ensure DiffUtil triggers updates properly
        super.submitList(list?.toList())
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onNoteClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvNoteTitle.text = note.title
            tvNoteMarks.text = note.marks?.let { "Grade: $it" } ?: "Not graded"
            tvNoteDate.text = note.createdAt?.let {
                DateFormat.format("MMM dd, yyyy", it).toString()
            } ?: "No date"

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
                    oldItem.imageUrls == newItem.imageUrls // added imageUrls check for completeness
    }
}

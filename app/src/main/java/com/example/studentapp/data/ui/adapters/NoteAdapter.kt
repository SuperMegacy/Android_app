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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding, onNoteClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<Note>?) {
        Log.d("NoteAdapter", "submitList called with size: ${list?.size ?: 0}")
        super.submitList(list) {
            Log.d("NoteAdapter", "submitList completed.")
        }
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onNoteClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvTitle.text = note.title
            tvGrade.text = note.marks?.let { "Grade: $it" } ?: "Not graded"

            root.setOnClickListener {
                Log.d("NoteAdapter", "Clicked note: ${note.id}")
                onNoteClick(note)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem == newItem
    }
}

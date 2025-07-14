package com.example.studentapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapp.data.model.Teacher
import com.example.studentapp.databinding.ItemTeacherBinding

class TeacherAdapter(
    private val teachers: MutableList<Teacher> = mutableListOf(),
    private val onTeacherClick: ((Teacher) -> Unit)? = null
) : RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

    inner class TeacherViewHolder(
        private val binding: ItemTeacherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(teacher: Teacher) {
            binding.tvTeacherName.text = "${teacher.firstName} ${teacher.lastName}"
            binding.tvTeacherEmail.text = teacher.email

            binding.root.setOnClickListener {
                onTeacherClick?.invoke(teacher)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val binding = ItemTeacherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TeacherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        holder.bind(teachers[position])
    }

    override fun getItemCount(): Int = teachers.size

    fun setTeachers(newTeachers: List<Teacher>) {
        teachers.clear()
        teachers.addAll(newTeachers)
        notifyDataSetChanged()
    }
}

package com.example.studentapp.data.ui.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studentapp.R

/**
 * Adapter for displaying a horizontal list of note images with remove functionality.
 * Uses Glide for efficient image loading and memory management.
 */
class NoteImageAdapter(
    private val context: Context,
    private val onImageRemoved: (Int) -> Unit
) : RecyclerView.Adapter<NoteImageAdapter.ImageViewHolder>() {

    private val imageUris = mutableListOf<Uri>()

    /**
     * Updates the entire list of images
     */
    fun submitList(newUris: List<Uri>) {
        imageUris.clear()
        imageUris.addAll(newUris)
        notifyDataSetChanged()
    }

    /**
     * Adds a single image to the list
     */
    fun addImage(uri: Uri) {
        imageUris.add(uri)
        notifyItemInserted(imageUris.size - 1)
    }

    /**
     * Removes an image at specified position
     */
    fun removeImage(position: Int) {
        if (position in imageUris.indices) {
            imageUris.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUris[position])
    }

    override fun getItemCount(): Int = imageUris.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(uri: Uri) {
            // Load image with Glide
            Glide.with(context)
                .load(uri)
                .centerCrop()
                .into(imagePreview)

            // Setup remove button click listener
            btnRemove.setOnClickListener {
                onImageRemoved(adapterPosition)
            }
        }
    }
}
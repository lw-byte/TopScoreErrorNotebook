package com.topscore.errornotebook.ui.profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.topscore.errornotebook.databinding.ItemTagBinding

class TagAdapter(
    private val onDeleteClick: (Tag) -> Unit
) : ListAdapter<Tag, TagAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(
        private val binding: ItemTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag) {
            binding.tvTagName.text = tag.name
            try {
                binding.viewTagColor.setBackgroundColor(Color.parseColor(tag.color))
            } catch (e: Exception) {
                binding.viewTagColor.setBackgroundColor(Color.GRAY)
            }
            binding.btnDeleteTag.setOnClickListener {
                onDeleteClick(tag)
            }
        }
    }

    private class TagDiffCallback : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem == newItem
        }
    }
}
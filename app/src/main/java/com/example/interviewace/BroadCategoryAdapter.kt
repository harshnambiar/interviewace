package com.example.interviewace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide


class BroadCategoryAdapter(
    private val categories: List<BroadCategory>,
    private val onClick: (BroadCategory) -> Unit
) : RecyclerView.Adapter<BroadCategoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.category_name)
        private val imageView: ImageView = itemView.findViewById(R.id.category_image)

        fun bind(category: BroadCategory) {
            nameText.text = category.name
            imageView.setImageResource(category.imageResId) // Load local drawable
            itemView.setOnClickListener { onClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_broad_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}
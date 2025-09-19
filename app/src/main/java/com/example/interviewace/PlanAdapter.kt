package com.example.interviewace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide


class PlanAdapter(
    private val plans: List<Plan>,
    private val onClick: (Plan) -> Unit
) : RecyclerView.Adapter<PlanAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.plan_name)
        private val imageView: ImageView = itemView.findViewById(R.id.plan_image)

        fun bind(plan: Plan) {
            nameText.text = plan.name
            imageView.setImageResource(plan.imageResId) // Load local drawable
            itemView.setOnClickListener { onClick(plan) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size
}
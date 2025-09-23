package com.example.interviewace

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ChatAdapter(private val messages: MutableList<Message>, private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }


    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.user_message_text)
        val cardView: CardView = itemView.findViewById(R.id.card_view)
    }

    class BotTextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.bot_message_text)
        val cardView: CardView = itemView.findViewById(R.id.card_view)
    }



    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_user_message, parent, false)
                UserMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bot_message, parent, false)
                BotTextMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val maxWidth = (context.resources.displayMetrics.widthPixels * 0.75f).toInt()

        when (holder) {
            is UserMessageViewHolder -> {
                holder.messageText.text = message.content
                (holder.cardView.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                    it.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    it.matchConstraintMaxWidth = maxWidth
                }
            }
            is BotTextMessageViewHolder -> {
                holder.messageText.text = message.content
                (holder.cardView.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                    it.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    it.matchConstraintMaxWidth = maxWidth
                }
            }

        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
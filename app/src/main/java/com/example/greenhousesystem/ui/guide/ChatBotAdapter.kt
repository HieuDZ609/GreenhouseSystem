package com.example.greenhousesystem.ui.guide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.greenhousesystem.R // Chỉnh lại R cho đúng
import com.example.greenhousesystem.model.ChatMessage

class ChatBotAdapter(private val messageList: List<ChatMessage>) :
    RecyclerView.Adapter<ChatBotAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutAi: LinearLayout = itemView.findViewById(R.id.layoutAiMessage)
        val tvAi: TextView = itemView.findViewById(R.id.tvAiMessage)

        val layoutUser: LinearLayout = itemView.findViewById(R.id.layoutUserMessage)
        val tvUser: TextView = itemView.findViewById(R.id.tvUserMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bot, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = messageList[position]

        if (chatMessage.isUser) {
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutAi.visibility = View.GONE
            holder.tvUser.text = chatMessage.message
        } else {
            holder.layoutAi.visibility = View.VISIBLE
            holder.layoutUser.visibility = View.GONE
            holder.tvAi.text = chatMessage.message
        }
    }

    override fun getItemCount(): Int = messageList.size
}
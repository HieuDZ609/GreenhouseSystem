package com.example.greenhousesystem.ui.notification

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.greenhousesystem.databinding.ItemNotificationBinding
import com.example.greenhousesystem.model.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit,
    private val onItemDelete: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            // Icon theo type
            binding.tvTypeIcon.text = when (notification.type) {
                "TEMPERATURE" -> "🌡️"
                "HUMIDITY" -> "💧"
                "DEVICE" -> "📡"
                else -> "🔔"
            }

            // Background icon theo type
            val bgColor = when (notification.type) {
                "TEMPERATURE" -> "#FFEBEE"
                "HUMIDITY" -> "#E3F2FD"
                "DEVICE" -> "#F3E5F5"
                else -> "#F5F5F5"
            }
            binding.tvTypeIcon.setBackgroundColor(Color.parseColor(bgColor))

            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTimestamp.text = formatTime(notification.timestamp)

            // Trạng thái đọc
            if (notification.isRead) {
                binding.tvReadStatus.text = "✓ Đã đọc"
                binding.tvReadStatus.setTextColor(Color.parseColor("#9E9E9E"))
                binding.tvTitle.setTypeface(null, Typeface.NORMAL)
                binding.viewUnreadIndicator.visibility = View.INVISIBLE
                binding.root.setCardBackgroundColor(Color.WHITE)
            } else {
                binding.tvReadStatus.text = "● Chưa đọc"
                binding.tvReadStatus.setTextColor(Color.parseColor("#2E7D32"))
                binding.tvTitle.setTypeface(null, Typeface.BOLD)
                binding.viewUnreadIndicator.visibility = View.VISIBLE
                binding.root.setCardBackgroundColor(Color.parseColor("#F9FBE7"))
            }

            // Click để xem chi tiết + đánh dấu đọc
            binding.root.setOnClickListener {
                onItemClick(notification)
            }
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return "--"
            return SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault())
                .format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Expose để ItemTouchHelper gọi khi swipe
    fun getItemAt(position: Int): Notification = getItem(position)

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(old: Notification, new: Notification) =
            old.id == new.id
        override fun areContentsTheSame(old: Notification, new: Notification) =
            old == new
    }
}
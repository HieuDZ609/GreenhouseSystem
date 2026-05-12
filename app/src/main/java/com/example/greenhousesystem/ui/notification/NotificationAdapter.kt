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
    private val onItemDelete: (Notification) -> Unit // Exposed for Swipe-to-delete
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            // Icon + màu nền biểu tượng theo loại cảnh báo (Eco-Digital Theme)
            val (iconTxt, bgColor) = when (notification.type) {
                "TEMPERATURE" -> "🌡️" to "#33FF4B4B" // Red Tint
                "HUMIDITY"    -> "💧" to "#330D47A1" // Blue Tint
                "DEVICE"      -> "📡" to "#33CCFF00" // Lime Tint
                else          -> "🔔" to "#33A0B8A8" // Gray Tint
            }

            binding.tvTypeIcon.text = iconTxt
            // Giả sử tvTypeIcon của bạn là một ShapeableImageView hoặc View có nền bo tròn
            // Hãy dùng GradientDrawable nếu cần tạo hình tròn, ở đây dùng parseColor trực tiếp
            binding.tvTypeIcon.setBackgroundColor(Color.parseColor(bgColor))

            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTimestamp.text = formatTime(notification.timestamp)

            // Kiểm tra trạng thái đã đọc để thay đổi font chữ và điểm màu (Indicator)
            if (notification.isRead) {
                binding.tvReadStatus.text = "✓ Đã đọc"
                binding.tvReadStatus.setTextColor(Color.parseColor("#A0B8A8")) // Xám nhạt
                binding.tvTitle.setTypeface(null, Typeface.NORMAL)
                binding.viewTypeIndicator.visibility = View.INVISIBLE
                binding.root.setCardBackgroundColor(Color.parseColor("#1AFFFFFF")) // Glass effect default
            } else {
                binding.tvReadStatus.text = "● Chưa đọc"
                binding.tvReadStatus.setTextColor(Color.parseColor("#CCFF00")) // Lime green
                binding.tvTitle.setTypeface(null, Typeface.BOLD)
                binding.viewTypeIndicator.visibility = View.VISIBLE
                binding.root.setCardBackgroundColor(Color.parseColor("#26CCFF00")) // Hơi sáng lên
            }

            // Bắt sự kiện click vào Card
            binding.root.setOnClickListener {
                onItemClick(notification)
            }
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return "--"
            return SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
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

    // Expose đối tượng để ItemTouchHelper trong Fragment biết đang thao tác với Notification nào.
    fun getItemAt(position: Int): Notification = getItem(position)

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}
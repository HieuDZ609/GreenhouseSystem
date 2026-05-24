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
            // 1. Xác định Level Icon và Màu sắc (Color Coding) dựa trên type
            val (iconLevel, tintColor) = when (notification.type) {
                "TEMP"  -> Pair(1, Color.parseColor("#FF7675")) // Soft Rose (Nhiệt độ)
                "HUMID" -> Pair(2, Color.parseColor("#74B9FF")) // Soft Blue (Độ ẩm)
                "BOTH"  -> Pair(3, Color.parseColor("#FAB1A0")) // Soft Pink (Cả hai - Nguy hiểm)
                else    -> Pair(0, Color.parseColor("#B2BEC3")) // Gray (Mặc định)
            }

            // 2. Kích hoạt SVG thay đổi hình dáng tự động qua level-list
            binding.ivTypeIcon.setImageLevel(iconLevel)

            // 3. Tint màu đồng bộ cho Icon và Dải viền bên trái
            binding.ivTypeIcon.setColorFilter(tintColor)

            // 4. Đổ dữ liệu Text
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTime.text = formatTime(notification.timestamp)

            // 5. Xử lý trạng thái Đọc / Chưa đọc (Bảo toàn Glassmorphism)
            if (notification.isRead) {
                // ĐÃ ĐỌC: Làm chìm thẻ kính xuống
                binding.tvTitle.setTypeface(null, Typeface.NORMAL)
                binding.tvTitle.setTextColor(Color.parseColor("#636E72")) // Chữ mờ đi
                binding.viewUnreadDot.visibility = View.GONE // Ẩn chấm đỏ

                // Giảm hiệu ứng bóng đổ và viền để thẻ "chìm" vào nền
                binding.root.cardElevation = 2f
                binding.root.strokeColor = Color.parseColor("#4DFFFFFF") // Viền trắng 30%
            } else {
                // CHƯA ĐỌC: Làm thẻ kính nổi bật lên
                binding.tvTitle.setTypeface(null, Typeface.BOLD)
                binding.tvTitle.setTextColor(Color.parseColor("#2D3436")) // Chữ Charcoal đậm
                binding.viewUnreadDot.visibility = View.VISIBLE // Hiện chấm Lavender

                // Tăng bóng đổ và viền sáng để thẻ "nổi" lên
                binding.root.cardElevation = 6f
                binding.root.strokeColor = Color.parseColor("#CCFFFFFF") // Viền trắng 80% bắt sáng
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
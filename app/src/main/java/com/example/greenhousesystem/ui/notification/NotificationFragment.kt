package com.example.greenhousesystem.ui.notification

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.greenhousesystem.databinding.FragmentNotificationBinding
import com.example.greenhousesystem.model.Notification
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { notification -> showDetailDialog(notification) },
            onItemDelete = { notification -> viewModel.deleteNotification(notification.id) }
        )

        binding.recyclerNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationFragment.adapter
        }

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            private val background = ColorDrawable(Color.parseColor("#D32F2F"))

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = adapter.getItemAt(position)
                viewModel.deleteNotification(notification.id)

                // Snackbar undo
                Snackbar.make(binding.root, "Đã xóa thông báo", Snackbar.LENGTH_LONG)
                    .setAction("Hoàn tác") {
                        // TODO: implement undo nếu cần
                    }.show()
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(canvas)
                super.onChildDraw(
                    canvas, recyclerView, viewHolder,
                    dX, dY, actionState, isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerNotifications)
    }

    private fun setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(binding.chipUnread.id) -> "UNREAD"
                checkedIds.contains(binding.chipTemperature.id) -> "TEMPERATURE"
                checkedIds.contains(binding.chipHumidity.id) -> "HUMIDITY"
                else -> "ALL"
            }
            viewModel.applyFilter(filter)
        }
    }

    private fun setupClickListeners() {
        // Đánh dấu tất cả đã đọc
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }

        // Xóa tất cả
        binding.fabDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }
    }

    private fun showDetailDialog(notification: Notification) {
        // Đánh dấu đã đọc
        if (!notification.isRead) {
            viewModel.markAsRead(notification.id)
        }

        // Icon theo type
        val icon = when (notification.type) {
            "TEMPERATURE" -> "🌡️"
            "HUMIDITY" -> "💧"
            "DEVICE" -> "📡"
            else -> "🔔"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$icon ${notification.title}")
            .setMessage(notification.message)
            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Xóa thông báo") { _, _ ->
                viewModel.deleteNotification(notification.id)
            }
            .show()
    }

    private fun showDeleteAllDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa tất cả thông báo")
            .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử thông báo không?\nHành động này không thể hoàn tác.")
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Xóa tất cả") { _, _ ->
                viewModel.deleteAllNotifications()
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.filteredNotifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            binding.tvUnreadCount.text = count.toString()
        }

        viewModel.totalCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalCount.text = count.toString()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerNotifications.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.fabDeleteAll.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearActionResult()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
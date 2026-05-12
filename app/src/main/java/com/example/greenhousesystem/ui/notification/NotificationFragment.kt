package com.example.greenhousesystem.ui.notification

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentNotificationBinding
import com.example.greenhousesystem.model.Notification
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * NotificationFragment — Màn hình lịch sử thông báo cảnh báo.
 *
 * ⚠️ LUỒNG DỮ LIỆU QUAN TRỌNG:
 * AlertWorker (chạy mỗi 15 phút) → Kiểm tra sensor vs threshold
 * → Nếu vượt ngưỡng → Ghi vào Firebase notifications/$uid
 * → NotificationViewModel listener nhận real-time update
 * → StateFlow emit UiState → Fragment collect và cập nhật UI.
 *
 * UiState handling:
 * Loading → Hiện ShimmerFrameLayout (skeleton loading)
 * Success → Ẩn shimmer, hiện RecyclerView với data
 * Empty   → Hiện empty state view (plant icon + text)
 * Error   → Snackbar thông báo lỗi
 *
 * Features:
 * - Filter chip: Tất cả / Chưa đọc / Nhiệt độ / Độ ẩm
 * - Swipe left to delete với undo Snackbar
 * - Click để xem chi tiết + auto mark as read
 * - FAB xóa tất cả với confirm dialog
 * - Staggered entrance animation khi load
 */
class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    // ViewModel ở Fragment scope (không cần share với Fragment khác)
    private val viewModel: NotificationViewModel by viewModels()

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

    // ─────────────────────────────────────────────────────────────────────
    //  RECYCLERVIEW + SWIPE TO DELETE
    // ─────────────────────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick  = { notification -> handleItemClick(notification) },
            onItemDelete = { notification -> viewModel.deleteNotification(notification.id) }
        )

        binding.recyclerNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationFragment.adapter
            // Tắt animation mặc định để dùng animation tùy chỉnh
            itemAnimator = null
        }

        // ── Swipe left để xóa ────────────────────────────────────────
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            private val deleteBackground = ColorDrawable(Color.parseColor("#8B1A1A"))

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = adapter.getItemAt(position)

                // Xóa item
                viewModel.deleteNotification(notification.id)

                // Snackbar undo (chú ý: sau khi xóa Firebase,
                // listener tự remove item → không cần rollback adapter thủ công)
                Snackbar.make(binding.root, "Đã xóa thông báo", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.parseColor("#1A2D1E"))
                    .setTextColor(Color.parseColor("#C8E6C9"))
                    .setActionTextColor(Color.parseColor("#CCFF00"))
                    .setAction("Hoàn tác") {
                        // TODO: Implement undo nếu cần (re-write to Firebase)
                    }.show()
            }

            // Vẽ nền đỏ khi đang swipe
            override fun onChildDraw(
                canvas: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX < 0) {  // Swipe trái
                    deleteBackground.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top + 8, itemView.right, itemView.bottom - 8
                    )
                    deleteBackground.draw(canvas)
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerNotifications)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  FILTER CHIPS — Xử lý khi user chọn filter
    // ─────────────────────────────────────────────────────────────────────
    private fun setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(binding.chipUnread.id)      -> NotifFilter.UNREAD
                checkedIds.contains(binding.chipTemperature.id) -> NotifFilter.TEMPERATURE
                checkedIds.contains(binding.chipHumidity.id)    -> NotifFilter.HUMIDITY
                else                                            -> NotifFilter.ALL
            }
            viewModel.applyFilter(filter)
        }
    }

    private fun setupClickListeners() {
        // Đánh dấu tất cả đã đọc
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }

        // FAB xóa tất cả
        binding.fabDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }

        // Empty state → navigate đến Threshold
        binding.btnGoToThreshold.setOnClickListener {
            // Navigate sẽ do NavController xử lý
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE VIEWMODEL — Dùng repeatOnLifecycle để an toàn với lifecycle
    //  repeatOnLifecycle(STARTED): tự pause khi Fragment bị che,
    //  resume khi quay lại → không leak memory, không update vô ích.
    // ─────────────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // ── UiState → điều khiển Shimmer + RecyclerView + Empty state ──
                launch {
                    viewModel.uiState.collectLatest { state ->
                        handleUiState(state)
                    }
                }

                // ── Stats: số chưa đọc + tổng ────────────────────────
                launch {
                    viewModel.unreadCount.collectLatest { count ->
                        binding.tvUnreadCount.text = count.toString()
                        // FAB chỉ hiện khi có thông báo
                    }
                }

                launch {
                    viewModel.totalCount.collectLatest { count ->
                        binding.tvTotalCount.text = count.toString()
                        binding.fabDeleteAll.visibility =
                            if (count > 0) View.VISIBLE else View.GONE
                    }
                }

                // ── Action messages (Snackbar) ────────────────────────
                launch {
                    viewModel.actionMessage.collectLatest { message ->
                        message?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT)
                                .setBackgroundTint(Color.parseColor("#1A2D1E"))
                                .setTextColor(Color.parseColor("#CCFF00"))
                                .show()
                            viewModel.clearActionMessage()
                        }
                    }
                }
            }
        }
    }

    /**
     * handleUiState — Chuyển đổi trạng thái UI dựa trên UiState.
     *
     * Loading: Shimmer ♦ RecyclerView GONE ♦ EmptyState GONE
     * Success: Shimmer GONE ♦ RecyclerView VISIBLE (submit list + animate)
     * Empty:   Shimmer GONE ♦ RecyclerView GONE ♦ EmptyState VISIBLE
     * Error:   Shimmer GONE ♦ Snackbar lỗi
     */
    private fun handleUiState(state: NotificationViewModel.UiState) {
        when (state) {
            is NotificationViewModel.UiState.Loading -> {
                // Bật Shimmer
                binding.shimmerNotifications.apply {
                    visibility = View.VISIBLE
                    startShimmer()
                }
                binding.recyclerNotifications.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
            }

            is NotificationViewModel.UiState.Success -> {
                // Tắt Shimmer, hiện RecyclerView
                binding.shimmerNotifications.apply {
                    stopShimmer()
                    visibility = View.GONE
                }
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerNotifications.visibility = View.VISIBLE

                // Submit list → DiffUtil tự tính animation
                adapter.submitList(state.notifications)

                // Staggered entrance nếu là lần đầu load
                if (adapter.itemCount == 0) {
                    animateListEntrance()
                }
            }

            is NotificationViewModel.UiState.Empty -> {
                binding.shimmerNotifications.apply {
                    stopShimmer()
                    visibility = View.GONE
                }
                binding.recyclerNotifications.visibility = View.GONE
                // Animate empty state fade in
                binding.layoutEmpty.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate().alpha(1f).setDuration(400L)
                        .setInterpolator(DecelerateInterpolator()).start()
                }
            }

            is NotificationViewModel.UiState.Error -> {
                binding.shimmerNotifications.apply {
                    stopShimmer()
                    visibility = View.GONE
                }
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.parseColor("#8B1A1A"))
                    .setTextColor(Color.parseColor("#FFCDD2"))
                    .show()
            }
        }
    }

    /**
     * animateListEntrance — Staggered animation cho các item đầu tiên.
     * Mỗi item slide-up + fade-in với delay tăng dần (60ms mỗi item).
     */
    private fun animateListEntrance() {
        binding.recyclerNotifications.post {
            val lm = binding.recyclerNotifications.layoutManager as LinearLayoutManager
            val first = lm.findFirstVisibleItemPosition()
            val last  = lm.findLastVisibleItemPosition()
            for (i in first..minOf(last, first + 5)) {
                val view = lm.findViewByPosition(i) ?: continue
                view.alpha = 0f
                view.translationY = 30f
                view.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(350L)
                    .setStartDelay((i - first) * 60L)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .start()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ITEM CLICK — Hiện dialog chi tiết + mark as read
    // ─────────────────────────────────────────────────────────────────────
    private fun handleItemClick(notification: Notification) {
        // Đánh dấu đã đọc ngay khi click
        if (!notification.isRead) {
            viewModel.markAsRead(notification.id)
        }

        val icon = when (notification.type) {
            "TEMPERATURE" -> "🌡️"
            "HUMIDITY"    -> "💧"
            else          -> "📡"
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.DarkDialogStyle)
            .setTitle("$icon ${notification.title}")
            .setMessage(notification.message)
            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Xóa") { _, _ ->
                viewModel.deleteNotification(notification.id)
            }
            .show()
    }

    private fun showDeleteAllDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.DarkDialogStyle)
            .setTitle("Xóa tất cả thông báo")
            .setMessage("Toàn bộ lịch sử cảnh báo sẽ bị xóa vĩnh viễn.\nHành động này không thể hoàn tác.")
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Xóa tất cả") { _, _ ->
                viewModel.deleteAllNotifications()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
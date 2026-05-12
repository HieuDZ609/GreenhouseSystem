package com.example.greenhousesystem.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhousesystem.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Enum filter khớp với Fragment
enum class NotifFilter { ALL, UNREAD, TEMPERATURE, HUMIDITY }

class NotificationViewModel : ViewModel() {

    // ── UiState định nghĩa các trạng thái UI ──
    sealed class UiState {
        object Loading : UiState()
        data class Success(val notifications: List<Notification>) : UiState()
        object Empty : UiState()
        data class Error(val message: String) : UiState()
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference.child("GreenHouseSystem")
    private val uid get() = auth.currentUser?.uid ?: ""
    private val notifRef get() = db.child("notifications").child(uid)

    // StateFlows thay vì LiveData
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Lưu trữ cache list nội bộ
    private var allNotificationsCache: List<Notification> = emptyList()
    private var currentFilter = NotifFilter.ALL
    private var notifListener: ValueEventListener? = null

    init {
        listenToNotifications()
    }

    /**
     * Lắng nghe Real-time từ Firebase
     */
    private fun listenToNotifications() {
        if (uid.isEmpty()) {
            _uiState.value = UiState.Error("Vui lòng đăng nhập.")
            return
        }

        notifListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Notification>()
                snapshot.children.forEach { child ->
                    try {
                        list.add(
                            Notification(
                                id = child.key ?: "",
                                title = child.child("title").getValue(String::class.java) ?: "",
                                message = child.child("message").getValue(String::class.java) ?: "",
                                type = child.child("type").getValue(String::class.java) ?: "DEVICE",
                                timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                                isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                            )
                        )
                    } catch (e: Exception) {
                        // Bỏ qua lỗi parsing cho 1 node cụ thể
                    }
                }

                // Sắp xếp thời gian giảm dần
                allNotificationsCache = list.sortedByDescending { it.timestamp }

                // Cập nhật số lượng
                _totalCount.value = allNotificationsCache.size
                _unreadCount.value = allNotificationsCache.count { !it.isRead }

                // Áp dụng filter và đẩy lên UI
                applyFilter(currentFilter)
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = UiState.Error("Lỗi kết nối: ${error.message}")
            }
        }

        // Lấy 100 thông báo gần nhất tránh giật lag UI
        notifRef.limitToLast(100).addValueEventListener(notifListener!!)
    }

    /**
     * Lọc danh sách thông báo
     */
    fun applyFilter(filter: NotifFilter) {
        currentFilter = filter
        val filtered = when (filter) {
            NotifFilter.UNREAD -> allNotificationsCache.filter { !it.isRead }
            NotifFilter.TEMPERATURE -> allNotificationsCache.filter { it.type == "TEMPERATURE" }
            NotifFilter.HUMIDITY -> allNotificationsCache.filter { it.type == "HUMIDITY" }
            NotifFilter.ALL -> allNotificationsCache
        }

        if (filtered.isEmpty()) {
            _uiState.value = UiState.Empty
        } else {
            _uiState.value = UiState.Success(filtered)
        }
    }

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notifRef.child(notificationId).child("isRead").setValue(true).await()
            } catch (e: Exception) {
                _actionMessage.value = "Lỗi cập nhật: ${e.message}"
            }
        }
    }

    /**
     * Đánh dấu tất cả đã đọc (Batch update)
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>()
                allNotificationsCache.filter { !it.isRead }.forEach { notif ->
                    updates["${notif.id}/isRead"] = true
                }
                if (updates.isNotEmpty()) {
                    notifRef.updateChildren(updates).await()
                    _actionMessage.value = "Đã đánh dấu tất cả đã đọc"
                }
            } catch (e: Exception) {
                _actionMessage.value = "Lỗi: ${e.message}"
            }
        }
    }

    /**
     * Xóa 1 thông báo cụ thể (Dùng cho thao tác vuốt xóa)
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notifRef.child(notificationId).removeValue().await()
                // Thông báo xoá thành công sẽ do Fragment lo qua Snackbar.
            } catch (e: Exception) {
                _actionMessage.value = "Lỗi xoá: ${e.message}"
            }
        }
    }

    /**
     * Xóa tất cả thông báo
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                notifRef.removeValue().await()
                _actionMessage.value = "Đã xóa toàn bộ lịch sử thông báo"
            } catch (e: Exception) {
                _actionMessage.value = "Lỗi xoá: ${e.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        notifListener?.let { notifRef.removeEventListener(it) }
    }
}
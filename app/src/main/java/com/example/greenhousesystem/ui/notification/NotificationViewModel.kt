package com.example.greenhousesystem.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhousesystem.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
        .child("GreenHouseSystem")

    private val uid get() = auth.currentUser?.uid ?: ""
    private val notifRef get() = db.child("notifications").child(uid)

    // Danh sách gốc từ Firebase
    private val _allNotifications = MutableLiveData<List<Notification>>()

    // Danh sách sau khi filter
    private val _filteredNotifications = MutableLiveData<List<Notification>>()
    val filteredNotifications: LiveData<List<Notification>> = _filteredNotifications

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> = _totalCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _actionResult = MutableLiveData<String?>()
    val actionResult: LiveData<String?> = _actionResult

    // Filter hiện tại: ALL / UNREAD / TEMPERATURE / HUMIDITY
    private var currentFilter = "ALL"

    private var notifListener: ValueEventListener? = null

    init {
        listenNotifications()
    }

    private fun listenNotifications() {
        _isLoading.value = true

        notifListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Notification>()

                snapshot.children.forEach { child ->
                    list.add(
                        Notification(
                            id = child.key ?: "",
                            title = child.child("title")
                                .getValue(String::class.java) ?: "",
                            message = child.child("message")
                                .getValue(String::class.java) ?: "",
                            type = child.child("type")
                                .getValue(String::class.java) ?: "TEMPERATURE",
                            timestamp = child.child("timestamp")
                                .getValue(Long::class.java) ?: 0L,
                            isRead = child.child("isRead")
                                .getValue(Boolean::class.java) ?: false
                        )
                    )
                }

                // Sắp xếp mới nhất lên đầu
                val sorted = list.sortedByDescending { it.timestamp }

                _allNotifications.value = sorted
                _totalCount.value = sorted.size
                _unreadCount.value = sorted.count { !it.isRead }
                _isLoading.value = false

                applyFilter(currentFilter)
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }

        notifRef.limitToLast(50)
            .addValueEventListener(notifListener!!)
    }

    fun applyFilter(filter: String) {
        currentFilter = filter
        val all = _allNotifications.value ?: emptyList()

        val filtered = when (filter) {
            "UNREAD" -> all.filter { !it.isRead }
            "TEMPERATURE" -> all.filter { it.type == "TEMPERATURE" }
            "HUMIDITY" -> all.filter { it.type == "HUMIDITY" }
            else -> all // ALL
        }

        _filteredNotifications.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    // Đánh dấu 1 thông báo đã đọc
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notifRef.child(notificationId)
                    .child("isRead")
                    .setValue(true)
                    .await()
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            }
        }
    }

    // Đánh dấu tất cả đã đọc
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val all = _allNotifications.value ?: return@launch
                val updates = mutableMapOf<String, Any>()

                all.filter { !it.isRead }.forEach { notif ->
                    updates["${notif.id}/isRead"] = true
                }

                if (updates.isNotEmpty()) {
                    notifRef.updateChildren(updates).await()
                    _actionResult.value = "Đã đánh dấu tất cả đã đọc"
                }
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            }
        }
    }

    // Xóa 1 thông báo
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notifRef.child(notificationId).removeValue().await()
                _actionResult.value = "Đã xóa thông báo"
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            }
        }
    }

    // Xóa tất cả thông báo
    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                notifRef.removeValue().await()
                _actionResult.value = "Đã xóa tất cả thông báo"
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        notifListener?.let { notifRef.removeEventListener(it) }
    }
}
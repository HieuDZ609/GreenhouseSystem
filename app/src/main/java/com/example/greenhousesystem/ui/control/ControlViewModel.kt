package com.example.greenhousesystem.ui.control

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhousesystem.model.LedMode
import com.example.greenhousesystem.model.LedStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ControlViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference
        .child("GreenHouseSystem")
        .child("devices")
        .child("led")

    private val _ledStatus = MutableLiveData<LedStatus>()
    val ledStatus: LiveData<LedStatus> = _ledStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _actionResult = MutableLiveData<String?>()
    val actionResult: LiveData<String?> = _actionResult

    private var ledListener: ValueEventListener? = null

    init {
        listenLedStatus()
    }

    private fun listenLedStatus() {
        ledListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _ledStatus.value = LedStatus(
                    isOn = snapshot.child("isOn")
                        .getValue(Boolean::class.java) ?: false,
                    red = snapshot.child("red")
                        .getValue(Int::class.java) ?: 0,
                    green = snapshot.child("green")
                        .getValue(Int::class.java) ?: 0,
                    blue = snapshot.child("blue")
                        .getValue(Int::class.java) ?: 0,
                    mode = snapshot.child("mode")
                        .getValue(String::class.java) ?: "MANUAL"
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(ledListener!!)
    }

    // Bật/tắt LED
    fun toggleLed(isOn: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                db.child("isOn").setValue(isOn).await()
                _actionResult.value = if (isOn) "Đã bật đèn LED" else "Đã tắt đèn LED"
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Chọn preset mode
    fun applyPresetMode(mode: LedMode) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updates = mapOf(
                    "mode" to mode.name,
                    "red" to mode.red,
                    "green" to mode.green,
                    "blue" to mode.blue,
                    "isOn" to true  // tự bật đèn khi chọn preset
                )
                db.updateChildren(updates).await()
                _actionResult.value = "Đã áp dụng chế độ ${mode.displayName}"
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Áp dụng màu manual
    fun applyManualColor(red: Int, green: Int, blue: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updates = mapOf(
                    "mode" to "MANUAL",
                    "red" to red,
                    "green" to green,
                    "blue" to blue
                )
                db.updateChildren(updates).await()
                _actionResult.value = "Đã áp dụng màu thủ công"
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ledListener?.let { db.removeEventListener(it) }
    }
}
package com.example.greenhousesystem.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.greenhousesystem.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeViewModel : ViewModel() {

    private val databaseUrl = "https://greenhousesystem-97224-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val db = FirebaseDatabase.getInstance(databaseUrl).reference
        .child("GreenHouseSystem")
    private val auth = FirebaseAuth.getInstance()

    // --- LiveData quan sát bởi HomeFragment ---
    private val _sensorData = MutableLiveData<SensorData>()
    val sensorData: LiveData<SensorData> = _sensorData

    private val _deviceStatus = MutableLiveData<DeviceStatus>()
    val deviceStatus: LiveData<DeviceStatus> = _deviceStatus

    private val _ledStatus = MutableLiveData<LedStatus>()
    val ledStatus: LiveData<LedStatus> = _ledStatus

    private val _selectedPlant = MutableLiveData<PlantProfile>()
    val selectedPlant: LiveData<PlantProfile> = _selectedPlant

    private val _alertMessage = MutableLiveData<String?>()
    val alertMessage: LiveData<String?> = _alertMessage

    // Quản lý Listener để tránh leak bộ nhớ
    private var telemetryListener: ValueEventListener? = null
    private var configListener: ValueEventListener? = null
    private var plantSelectionListener: ValueEventListener? = null
    private var currentDeviceId: String? = null

    init {
        listenSelectedPlant() // Luôn lắng nghe loại cây người dùng đang chọn
    }

    /**
     * Được gọi khi SharedDeviceViewModel báo có thiết bị mới được chọn
     */
    fun loadDeviceData(deviceId: String) {
        if (currentDeviceId == deviceId) return

        // Gỡ bỏ listener cũ nếu đang đổi thiết bị
        removeOldListeners()

        currentDeviceId = deviceId
        observeTelemetry(deviceId)
        observeLedConfig(deviceId)
    }

    private fun observeTelemetry(deviceId: String) {
        telemetryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Đọc dữ liệu cảm biến [cite: 16, 41, 99]
                val temp = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
                val humid = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0
                val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                _sensorData.value = SensorData(temp, humid, ts)

                // Đọc trạng thái Online/Offline
                val isOnline = snapshot.child("is_online").getValue(Boolean::class.java) ?: false
                _deviceStatus.value = DeviceStatus(
                    status = if (isOnline) "online" else "offline",
                    lastSeen = ts
                )

                // Kiểm tra cảnh báo dựa trên ngưỡng của loại cây đang chọn [cite: 12, 38, 151]
                checkThresholds(temp, humid)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("Telemetry").child(deviceId).addValueEventListener(telemetryListener!!)
    }

    private fun observeLedConfig(deviceId: String) {
        configListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOn = snapshot.child("led_status").getValue(Boolean::class.java) ?: false
                val mode = snapshot.child("led_mode").getValue(String::class.java) ?: "MANUAL"

                // Lấy màu từ node led_color {r, g, b}
                val r = snapshot.child("led_color").child("r").getValue(Int::class.java) ?: 0
                val g = snapshot.child("led_color").child("g").getValue(Int::class.java) ?: 0
                val b = snapshot.child("led_color").child("b").getValue(Int::class.java) ?: 0

                _ledStatus.value = LedStatus(isOn, r, g, b, mode)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("Configs").child(deviceId).addValueEventListener(configListener!!)
    }

    private fun listenSelectedPlant() {
        val uid = auth.currentUser?.uid ?: return
        plantSelectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val plantId = snapshot.getValue(String::class.java) ?: "TOMATO"
                fetchPlantProfile(plantId)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("users").child(uid).child("selectedPlant")
            .addValueEventListener(plantSelectionListener!!)
    }

    private fun fetchPlantProfile(plantId: String) {
        db.child("PlantProfiles").child(plantId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                _selectedPlant.value = PlantProfile(
                    id = plantId,
                    name = snapshot.child("name").getValue(String::class.java) ?: "",
                    icon = snapshot.child("icon").getValue(String::class.java) ?: "",
                    tempMin = snapshot.child("tempMin").getValue(Double::class.java) ?: 0.0,
                    tempMax = snapshot.child("tempMax").getValue(Double::class.java) ?: 0.0,
                    humidityMin = snapshot.child("humidityMin").getValue(Double::class.java) ?: 0.0,
                    humidityMax = snapshot.child("humidityMax").getValue(Double::class.java) ?: 0.0,
                    recommendedLedMode = snapshot.child("recommendedLedMode").getValue(String::class.java) ?: "VEGETATIVE"
                )
            }
        }
    }

    // Gửi lệnh bật/tắt đèn lên Firebase [cite: 34, 126]
    fun toggleLed(deviceId: String, newState: Boolean) {
        db.child("Configs").child(deviceId).child("led_status").setValue(newState)
    }

    private fun checkThresholds(temp: Double, humid: Double) {
        val plant = _selectedPlant.value ?: return
        val message = when {
            temp > plant.tempMax -> "🌡️ Nhiệt độ quá cao (${temp}°C)! Ngưỡng tối đa: ${plant.tempMax}°C"
            temp < plant.tempMin -> "🌡️ Nhiệt độ quá thấp (${temp}°C)! Ngưỡng tối thiểu: ${plant.tempMin}°C"
            humid > plant.humidityMax -> "💧 Độ ẩm quá cao (${humid}%)!"
            humid < plant.humidityMin -> "💧 Độ ẩm quá thấp (${humid}%)!"
            else -> null
        }
        _alertMessage.value = message
    }

    private fun removeOldListeners() {
        currentDeviceId?.let { id ->
            telemetryListener?.let { db.child("Telemetry").child(id).removeEventListener(it) }
            configListener?.let { db.child("Configs").child(id).removeEventListener(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeOldListeners()
        auth.currentUser?.uid?.let { uid ->
            plantSelectionListener?.let { db.child("users").child(uid).child("selectedPlant").removeEventListener(it) }
        }
    }
}
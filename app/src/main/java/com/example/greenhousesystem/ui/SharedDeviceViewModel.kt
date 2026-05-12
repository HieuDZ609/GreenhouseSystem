package com.example.greenhousesystem.ui

import android.util.Log
import androidx.lifecycle.*
import com.example.greenhousesystem.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * SharedDeviceViewModel — "Single Source of Truth" cho toàn bộ ứng dụng.
 *
 * Được khởi tạo ở Activity scope (MainActivity) thông qua `by viewModels()`.
 * Tất cả Fragment (Home, Control, Chart) truy cập bằng `by activityViewModels()`.
 *
 * Lợi ích kiến trúc:
 * - Firebase listener chỉ được đăng ký 1 lần duy nhất.
 * - Khi chuyển tab, dữ liệu không bị load lại → tiết kiệm băng thông.
 * - StateFlow giúp Fragment luôn nhận được giá trị mới nhất ngay khi subscribe.
 *
 * Dữ liệu quản lý:
 * - [sensorData]     : Nhiệt độ + Độ ẩm realtime từ ESP32
 * - [deviceStatus]   : Online/Offline + lastSeen timestamp
 * - [ledStatus]      : Trạng thái LED RGB (on/off, R, G, B, mode)
 * - [selectedPlant]  : Cây đang được chọn của user hiện tại
 * - [alertMessage]   : Chuỗi cảnh báo nếu sensor vượt ngưỡng
 * - [thresholds]     : Ngưỡng nhiệt độ/độ ẩm (custom hoặc từ plant profile)
 */
class SharedDeviceViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference.child("GreenHouseSystem")
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    // ── StateFlow (thay LiveData cho Coroutine Flow support) ─────────────
    // StateFlow luôn giữ giá trị cuối cùng → Fragment nhận ngay khi subscribe.

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _ledStatus = MutableStateFlow(LedStatus())
    val ledStatus: StateFlow<LedStatus> = _ledStatus.asStateFlow()

    private val _selectedPlant = MutableStateFlow<PlantProfile?>(null)
    val selectedPlant: StateFlow<PlantProfile?> = _selectedPlant.asStateFlow()

    private val _thresholds = MutableStateFlow(ThresholdConfig())
    val thresholds: StateFlow<ThresholdConfig> = _thresholds.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    // ── Firebase listeners (cần lưu lại để remove khi ViewModel destroyed) ──
    private var sensorListener: ValueEventListener? = null
    private var deviceListener: ValueEventListener? = null
    private var ledListener: ValueEventListener? = null
    private var plantListener: ValueEventListener? = null
    private var thresholdListener: ValueEventListener? = null
    private var savedUid: String? = null


    init {
        startListening()
    }

    /**
     * startListening — Đăng ký tất cả Firebase realtime listeners.
     * Gọi 1 lần trong init{}, các listener tự cập nhật StateFlow khi có data.
     */
    fun startListening() {
        savedUid = uid
        listenSensor()
        listenDeviceStatus()
        listenLedStatus()
        listenSelectedPlant()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SENSOR LISTENER — Nhận nhiệt độ + độ ẩm realtime từ ESP32
    // ─────────────────────────────────────────────────────────────────────
    private fun listenSensor() {
        sensorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = SensorData(
                    temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0,
                    humidity    = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0,
                    timestamp   = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                )
                _sensorData.value = data
                checkAlerts(data)   // tự động kiểm tra ngưỡng mỗi khi có data mới
            }
            override fun onCancelled(error: DatabaseError) {
                // Emit trạng thái offline để UI phản ứng
                _deviceStatus.value = DeviceStatus(status = "offline", lastSeen = 0L)
                // Log để debug
                Log.e("Firebase", "Listener cancelled: ${error.message}")
            }

        }
        db.child("sensors").addValueEventListener(sensorListener!!)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DEVICE STATUS LISTENER
    // ─────────────────────────────────────────────────────────────────────
    private fun listenDeviceStatus() {
        deviceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _deviceStatus.value = DeviceStatus(
                    status   = snapshot.child("status").getValue(String::class.java) ?: "offline",
                    lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                )
            }
            override fun onCancelled(error: DatabaseError) {
                // Emit trạng thái offline để UI phản ứng
                _deviceStatus.value = DeviceStatus(status = "offline", lastSeen = 0L)
                // Log để debug
                Log.e("Firebase", "Listener cancelled: ${error.message}")
            }

        }
        db.child("devices").addValueEventListener(deviceListener!!)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LED STATUS LISTENER
    // ─────────────────────────────────────────────────────────────────────
    private fun listenLedStatus() {
        ledListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnRaw = snapshot.child("isOn").value
                val parsedIsOn = when (isOnRaw) {
                    is Boolean -> isOnRaw
                    is String -> isOnRaw == "true"
                    is Number -> isOnRaw.toInt() == 1
                    else -> false
                }
                _ledStatus.value = LedStatus(
                    isOn  = parsedIsOn,
                    red   = snapshot.child("red").getValue(Int::class.java) ?: 0,
                    green = snapshot.child("green").getValue(Int::class.java) ?: 0,
                    blue  = snapshot.child("blue").getValue(Int::class.java) ?: 0,
                    mode  = snapshot.child("mode").getValue(String::class.java) ?: "MANUAL"
                )
            }
            override fun onCancelled(error: DatabaseError) {
                // Emit trạng thái offline để UI phản ứng
                _deviceStatus.value = DeviceStatus(status = "offline", lastSeen = 0L)
                // Log để debug
                Log.e("Firebase", "Listener cancelled: ${error.message}")
            }

        }
        db.child("devices").child("led").addValueEventListener(ledListener!!)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SELECTED PLANT LISTENER — Khi user đổi cây, load lại threshold
    // ─────────────────────────────────────────────────────────────────────
    private fun listenSelectedPlant() {
        if (uid.isEmpty()) return
        plantListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val plantId = snapshot.getValue(String::class.java) ?: "TOMATO"
                loadPlantProfile(plantId)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("users").child(uid).child("selectedPlant")
            .addValueEventListener(plantListener!!)
    }

    private fun loadPlantProfile(plantId: String) {
        viewModelScope.launch {
            try {
                val snap = db.child("plantProfiles").child(plantId).get().await()
                val profile = PlantProfile(
                    id                = plantId,
                    name              = snap.child("name").getValue(String::class.java) ?: "",
                    icon              = snap.child("icon").getValue(String::class.java) ?: "",
                    tempMin           = snap.child("tempMin").getValue(Double::class.java) ?: 15.0,
                    tempMax           = snap.child("tempMax").getValue(Double::class.java) ?: 35.0,
                    humidityMin       = snap.child("humidityMin").getValue(Double::class.java) ?: 40.0,
                    humidityMax       = snap.child("humidityMax").getValue(Double::class.java) ?: 90.0,
                    recommendedLedMode = snap.child("recommendedLedMode").getValue(String::class.java) ?: "VEGETATIVE"
                )
                _selectedPlant.value = profile

                // Đọc customThresholds của user (ưu tiên hơn plant default)
                loadThresholds(profile)
            } catch (_: Exception) {}
        }
    }

    /**
     * loadThresholds — Ưu tiên dùng customThresholds của user.
     * Nếu không có custom → dùng giá trị từ plant profile.
     */
    private suspend fun loadThresholds(plant: PlantProfile) {
        if (uid.isEmpty()) return
        val customSnap = db.child("users").child(uid).child("customThresholds").get().await()
        _thresholds.value = if (customSnap.exists()) {
            ThresholdConfig(
                tempMin   = customSnap.child("tempMin").getValue(Double::class.java) ?: plant.tempMin,
                tempMax   = customSnap.child("tempMax").getValue(Double::class.java) ?: plant.tempMax,
                humidMin  = customSnap.child("humidityMin").getValue(Double::class.java) ?: plant.humidityMin,
                humidMax  = customSnap.child("humidityMax").getValue(Double::class.java) ?: plant.humidityMax
            )
        } else {
            ThresholdConfig(plant.tempMin, plant.tempMax, plant.humidityMin, plant.humidityMax)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ALERT CHECK — So sánh sensor với threshold, emit chuỗi cảnh báo
    // ─────────────────────────────────────────────────────────────────────
    private fun checkAlerts(data: SensorData) {
        val t = _thresholds.value
        val alerts = mutableListOf<String>()
        when {
            data.temperature > t.tempMax ->
                alerts += "🌡️ Nhiệt độ ${data.temperature}°C vượt ngưỡng (>${t.tempMax}°C)"
            data.temperature < t.tempMin ->
                alerts += "🌡️ Nhiệt độ ${data.temperature}°C thấp hơn ngưỡng (<${t.tempMin}°C)"
        }
        when {
            data.humidity > t.humidMax ->
                alerts += "💧 Độ ẩm ${data.humidity}% vượt ngưỡng (>${t.humidMax}%)"
            data.humidity < t.humidMin ->
                alerts += "💧 Độ ẩm ${data.humidity}% thấp hơn ngưỡng (<${t.humidMin}%)"
        }
        _alertMessage.value = alerts.joinToString("\n").ifEmpty { null }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LED CONTROL — Toggle và apply màu/mode
    // ─────────────────────────────────────────────────────────────────────
    fun toggleLed(isOn: Boolean) {
        val updates = mapOf("isOn" to isOn)
        db.child("devices").child("led").updateChildren(updates)
            .addOnSuccessListener {
                // Nếu thành công, Android Studio sẽ in dòng này ra tab Logcat
                Log.d("FIREBASE_LED", "✅ Đã gửi lệnh thành công. isOn mới = $isOn")
            }
            .addOnFailureListener { error ->
                // Nếu Firebase từ chối, nó sẽ báo rõ lý do tại đây
                Log.e("FIREBASE_LED", "❌ Lỗi không thể đổi trạng thái: ${error.message}")
            }    }

    fun applyLedMode(mode: LedMode) {
        val updates = mapOf(
            "mode" to mode.name, "red" to mode.red,
            "green" to mode.green, "blue" to mode.blue, "isOn" to true
        )
        db.child("devices").child("led").updateChildren(updates)
    }

    fun applyManualColor(r: Int, g: Int, b: Int) {
        val updates = mapOf("mode" to "MANUAL", "red" to r, "green" to g, "blue" to b, "isOn" to true)

        db.child("devices").child("led").updateChildren(updates)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  THRESHOLD SAVE — Lưu ngưỡng tùy chỉnh của user lên Firebase
    // ─────────────────────────────────────────────────────────────────────
    fun saveThresholds(tempMin: Double, tempMax: Double, humidMin: Double, humidMax: Double,
                       onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (uid.isEmpty()) return
        val updates = mapOf(
            "tempMin" to tempMin, "tempMax" to tempMax,
            "humidityMin" to humidMin, "humidityMax" to humidMax
        )
        db.child("users").child(uid).child("customThresholds")
            .updateChildren(updates)
            .addOnSuccessListener {
                _thresholds.value = ThresholdConfig(tempMin, tempMax, humidMin, humidMax)
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Lỗi lưu ngưỡng") }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PLANT SELECTION — Lưu cây được chọn lên Firebase
    // ─────────────────────────────────────────────────────────────────────
    fun selectPlant(plantId: String) {
        if (uid.isEmpty()) return
        db.child("users").child(uid).child("selectedPlant").setValue(plantId)
        // Listener sẽ tự gọi loadPlantProfile → cập nhật _selectedPlant và _thresholds
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CLEANUP — Remove tất cả listeners khi ViewModel bị destroy
    // ─────────────────────────────────────────────────────────────────────
    override fun onCleared() {
        super.onCleared()
        sensorListener?.let { db.child("sensors").removeEventListener(it) }
        deviceListener?.let { db.child("devices").removeEventListener(it) }
        ledListener?.let { db.child("devices").child("led").removeEventListener(it) }
        savedUid?.let { uid ->
            plantListener?.let {
                db.child("users").child(uid).child("selectedPlant").removeEventListener(it)
            }
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Data models nhỏ dùng nội bộ (đặt chung file cho tiện)
// ─────────────────────────────────────────────────────────────────────────
data class ThresholdConfig(
    val tempMin : Double = 15.0,
    val tempMax : Double = 35.0,
    val humidMin: Double = 40.0,
    val humidMax: Double = 90.0
)
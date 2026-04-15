package com.example.greenhousesystem.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.greenhousesystem.model.DeviceStatus
import com.example.greenhousesystem.model.LedStatus
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.model.SensorData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference
        .child("GreenHouseSystem")
    private val auth = FirebaseAuth.getInstance()

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

    // Listeners để remove khi ViewModel bị destroy
    private var sensorListener: ValueEventListener? = null
    private var deviceListener: ValueEventListener? = null
    private var ledListener: ValueEventListener? = null
    private var plantListener: ValueEventListener? = null

    init {
        listenSensor()
        listenDeviceStatus()
        listenLedStatus()
        listenSelectedPlant()
    }

    private fun listenSensor() {
        sensorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = SensorData(
                    temperature = snapshot.child("temperature")
                        .getValue(Double::class.java) ?: 0.0,
                    humidity = snapshot.child("humidity")
                        .getValue(Double::class.java) ?: 0.0,
                    timestamp = snapshot.child("timestamp")
                        .getValue(Long::class.java) ?: 0L
                )
                _sensorData.value = data
                checkAlert(data)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("sensors").addValueEventListener(sensorListener!!)
    }

    private fun listenDeviceStatus() {
        deviceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _deviceStatus.value = DeviceStatus(
                    status = snapshot.child("status")
                        .getValue(String::class.java) ?: "offline",
                    lastSeen = snapshot.child("lastSeen")
                        .getValue(Long::class.java) ?: 0L
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("devices").addValueEventListener(deviceListener!!)
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
        db.child("devices").child("led").addValueEventListener(ledListener!!)
    }

    private fun listenSelectedPlant() {
        val uid = auth.currentUser?.uid ?: return
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
        db.child("plantProfiles").child(plantId)
            .get().addOnSuccessListener { snapshot ->
                _selectedPlant.value = PlantProfile(
                    id = plantId,
                    name = snapshot.child("name").getValue(String::class.java) ?: "",
                    icon = snapshot.child("icon").getValue(String::class.java) ?: "",
                    tempMin = snapshot.child("tempMin").getValue(Double::class.java) ?: 0.0,
                    tempMax = snapshot.child("tempMax").getValue(Double::class.java) ?: 0.0,
                    humidityMin = snapshot.child("humidityMin").getValue(Double::class.java) ?: 0.0,
                    humidityMax = snapshot.child("humidityMax").getValue(Double::class.java) ?: 0.0,
                    recommendedLedMode = snapshot.child("recommendedLedMode")
                        .getValue(String::class.java) ?: "VEGETATIVE"
                )
            }
    }

    private fun checkAlert(data: SensorData) {
        val plant = _selectedPlant.value ?: return
        val alerts = mutableListOf<String>()

        when {
            data.temperature > plant.tempMax ->
                alerts.add("🌡️ Nhiệt độ ${data.temperature}°C vượt ngưỡng (>${plant.tempMax}°C)")
            data.temperature < plant.tempMin ->
                alerts.add("🌡️ Nhiệt độ ${data.temperature}°C thấp hơn ngưỡng (<${plant.tempMin}°C)")
        }
        when {
            data.humidity > plant.humidityMax ->
                alerts.add("💧 Độ ẩm ${data.humidity}% vượt ngưỡng (>${plant.humidityMax}%)")
            data.humidity < plant.humidityMin ->
                alerts.add("💧 Độ ẩm ${data.humidity}% thấp hơn ngưỡng (<${plant.humidityMin}%)")
        }

        _alertMessage.value = if (alerts.isEmpty()) null else alerts.joinToString("\n")
    }

    fun toggleLed(currentState: Boolean) {
        db.child("devices").child("led").child("isOn").setValue(!currentState)
    }

    override fun onCleared() {
        super.onCleared()
        sensorListener?.let { db.child("sensors").removeEventListener(it) }
        deviceListener?.let { db.child("devices").removeEventListener(it) }
        ledListener?.let { db.child("devices").child("led").removeEventListener(it) }
        val uid = auth.currentUser?.uid ?: return
        plantListener?.let {
            db.child("users").child(uid).child("selectedPlant").removeEventListener(it)
        }
    }
}
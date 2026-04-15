package com.example.greenhousesystem.ui.chart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.model.SensorHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class ChartViewModel : ViewModel() {

    private val databaseUrl = "https://greenhousesystem-97224-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val db = FirebaseDatabase.getInstance(databaseUrl).reference
        .child("GreenHouseSystem")
    private val auth = FirebaseAuth.getInstance()

    private val _historyData = MutableLiveData<List<SensorHistory>>()
    val historyData: LiveData<List<SensorHistory>> = _historyData

    private val _selectedPlant = MutableLiveData<PlantProfile>()
    val selectedPlant: LiveData<PlantProfile> = _selectedPlant

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private var historyListener: ValueEventListener? = null
    private var plantListener: ValueEventListener? = null

    init {
        listenSelectedPlant()
        loadHistory()
    }

    fun loadHistory() {
        _isLoading.value = true

        // Lấy dữ liệu 1 giờ gần nhất
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)

        historyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<SensorHistory>()

                snapshot.children.forEach { child ->
                    val timestamp = child.child("timestamp")
                        .getValue(Long::class.java) ?: 0L

                    // Chỉ lấy dữ liệu trong 1 giờ gần nhất
                    if (timestamp >= oneHourAgo) {
                        list.add(
                            SensorHistory(
                                temperature = child.child("temperature")
                                    .getValue(Double::class.java) ?: 0.0,
                                humidity = child.child("humidity")
                                    .getValue(Double::class.java) ?: 0.0,
                                timestamp = timestamp
                            )
                        )
                    }
                }

                // Sắp xếp theo thời gian tăng dần
                val sorted = list.sortedBy { it.timestamp }

                _isLoading.value = false
                _isEmpty.value = sorted.isEmpty()

                if (sorted.isEmpty()) {
                    // Dùng mock data nếu chưa có dữ liệu thật
                    _historyData.value = generateMockData()
                } else {
                    _historyData.value = sorted
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
                _historyData.value = generateMockData()
            }
        }

        db.child("sensorHistory")
            .orderByChild("timestamp")
            .limitToLast(60) // tối đa 60 điểm
            .addValueEventListener(historyListener!!)
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
        db.child("plantProfiles").child(plantId).get()
            .addOnSuccessListener { snapshot ->
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

    // Mock data để test chart khi chưa có ESP32
    private fun generateMockData(): List<SensorHistory> {
        val list = mutableListOf<SensorHistory>()
        val now = System.currentTimeMillis()
        var temp = 27.0
        var humid = 65.0

        for (i in 59 downTo 0) {
            temp += (Math.random() - 0.5) * 1.5
            humid += (Math.random() - 0.5) * 3.0
            temp = temp.coerceIn(20.0, 38.0)
            humid = humid.coerceIn(40.0, 95.0)

            list.add(
                SensorHistory(
                    // SỬA TẠI ĐÂY: Dùng Locale.US để ép buộc dùng dấu chấm (.)
                    temperature = String.format(java.util.Locale.US, "%.1f", temp).toDouble(),
                    humidity = String.format(java.util.Locale.US, "%.1f", humid).toDouble(),
                    timestamp = now - (i * 60 * 1000L)
                )
            )
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.let {
            db.child("sensorHistory").removeEventListener(it)
        }
        val uid = auth.currentUser?.uid ?: return
        plantListener?.let {
            db.child("users").child(uid).child("selectedPlant").removeEventListener(it)
        }
    }
}
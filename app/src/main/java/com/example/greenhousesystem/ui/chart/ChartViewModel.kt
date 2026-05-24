package com.example.greenhousesystem.ui.chart

import androidx.lifecycle.*
import com.google.firebase.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlinx.coroutines.withTimeout

class ChartViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference
        .child("GreenHouseSystem").child("sensorHistory")

    // ── Filter hiện tại ───────────────────────────────────────────────
    private val _currentFilter = MutableStateFlow(ChartFilter.TODAY)
    val currentFilter: StateFlow<ChartFilter> = _currentFilter.asStateFlow()

    // ── Dữ liệu chart sau khi group ──────────────────────────────────
    private val _chartDataTemp = MutableStateFlow<List<ChartEntry>>(emptyList())
    val chartDataTemp: StateFlow<List<ChartEntry>> = _chartDataTemp.asStateFlow()

    private val _chartDataHumid = MutableStateFlow<List<ChartEntry>>(emptyList())
    val chartDataHumid: StateFlow<List<ChartEntry>> = _chartDataHumid.asStateFlow()

    // ── Thống kê min / avg / max ──────────────────────────────────────
    private val _tempStats = MutableStateFlow(SensorStats())
    val tempStats: StateFlow<SensorStats> = _tempStats.asStateFlow()

    private val _humidStats = MutableStateFlow(SensorStats())
    val humidStats: StateFlow<SensorStats> = _humidStats.asStateFlow()

    // ── Loading & Mock flag ───────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isMockData = MutableStateFlow(false)
    val isMockData: StateFlow<Boolean> = _isMockData.asStateFlow()

    // ── Threshold để vẽ LimitLine trên chart ─────────────────────────
    private val _tempThreshold = MutableStateFlow(Pair(15.0, 35.0))
    val tempThreshold: StateFlow<Pair<Double, Double>> = _tempThreshold.asStateFlow()

    private val _humidThreshold = MutableStateFlow(Pair(40.0, 90.0))
    val humidThreshold: StateFlow<Pair<Double, Double>> = _humidThreshold.asStateFlow()

    private val _isEmptyState = MutableStateFlow(false)
    val isEmptyState: StateFlow<Boolean> = _isEmptyState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchData(ChartFilter.TODAY)
    }

    fun setFilter(filter: ChartFilter) {
        if (_currentFilter.value == filter) return
        _currentFilter.value = filter
        fetchData(filter)
    }

    fun setThresholds(
        tempMin: Double, tempMax: Double,
        humidMin: Double, humidMax: Double
    ) {
        _tempThreshold.value  = Pair(tempMin, tempMax)
        _humidThreshold.value = Pair(humidMin, humidMax)
    }

    fun refresh() = fetchData(_currentFilter.value)

    fun fetchData(filter: ChartFilter = _currentFilter.value) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val fromTime = when (filter) {
                    ChartFilter.TODAY -> now - 24L * 60 * 60 * 1000
                    ChartFilter.WEEK  -> now - 7L  * 24 * 60 * 60 * 1000
                    ChartFilter.MONTH -> now - 30L * 24 * 60 * 60 * 1000
                }

                val snapshot = db
                    .orderByChild("timestamp")
                    .startAt(fromTime.toDouble())
                    .get().await()

                val rawList = mutableListOf<HistoryRecord>()
                snapshot.children.forEach { child ->
                    val temp  = child.child("temperature").getValue(Double::class.java) ?: return@forEach
                    val humid = child.child("humidity").getValue(Double::class.java)    ?: return@forEach
                    val ts    = child.child("timestamp").getValue(Long::class.java)     ?: return@forEach
                    rawList.add(HistoryRecord(temp, humid, ts))
                }

                if (rawList.isEmpty()) {
                    _isMockData.value = true
                    _isEmptyState.value = true
                    processAndEmit(emptyList(), filter)
                } else {
                    _isMockData.value = false
                    _isEmptyState.value = false
                    processAndEmit(rawList, filter)
                }
            } catch (e: Exception) {
                _isMockData.value = true
                _isEmptyState.value = false
                _errorMessage.value = "Lỗi kết nối Firebase: ${e.message}"
                processAndEmit(emptyList(), filter)
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun processAndEmit(data: List<HistoryRecord>, filter: ChartFilter) {
        val sortedData = data.sortedBy { it.timestamp }
        val cal = Calendar.getInstance()

        val filteredRecords = mutableListOf<HistoryRecord>()
        val chartEntriesTemp = mutableListOf<ChartEntry>()
        val chartEntriesHumid = mutableListOf<ChartEntry>()
        if (sortedData.isNotEmpty()){
        when (filter) {
            ChartFilter.TODAY -> {
                // Chế độ 1 ngày: Gom nhóm theo từng Giờ (00:00, 01:00, ...) như cũ
                val groupKey: (Long) -> String = { timestamp ->
                    cal.timeInMillis = timestamp
                    String.format("%02d:00", cal.get(Calendar.HOUR_OF_DAY))
                }
                val grouped = sortedData.groupBy { groupKey(it.timestamp) }

                grouped.forEach { (label, items) ->
                    chartEntriesTemp.add(ChartEntry(label, items.map { it.temperature }.average().toFloat()))
                    chartEntriesHumid.add(ChartEntry(label, items.map { it.humidity }.average().toFloat()))
                }
                // Thống kê Stats tính trên toàn bộ dữ liệu thô trong ngày
                filteredRecords.addAll(sortedData)
            }

            ChartFilter.WEEK -> {
                // Chế độ 7 ngày: Lấy mẫu nốt cách nhau ít nhất 2 giờ (2 * 60 * 60 * 1000 ms)
                val interval = 2 * 60 * 60 * 1000L
                var lastTimestamp = 0L

                sortedData.forEach { record ->
                    if ((record.timestamp - lastTimestamp) >= interval) {
                        cal.timeInMillis = record.timestamp
                        // Định dạng nhãn hiển thị: "Giờ:00 Ngày/Tháng" (Ví dụ: 14h 20/05)
                        val label = String.format("%02dh %02d/%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)

                        chartEntriesTemp.add(ChartEntry(label, record.temperature.toFloat()))
                        chartEntriesHumid.add(ChartEntry(label, record.humidity.toFloat()))

                        filteredRecords.add(record)
                        lastTimestamp = record.timestamp
                    }
                }
            }

            ChartFilter.MONTH -> {
                // Chế độ 30 ngày: Lấy mẫu nốt cách nhau ít nhất 12 giờ (12 * 60 * 60 * 1000 ms)
                val interval = 12 * 60 * 60 * 1000L
                var lastTimestamp = 0L

                sortedData.forEach { record ->
                    if ((record.timestamp - lastTimestamp) >= interval) {
                        cal.timeInMillis = record.timestamp
                        // Định dạng nhãn hiển thị: "Buổi Ngày/Tháng" (Ví dụ: Sáng 20/05 hoặc Chiều 20/05)
                        val session = if (cal.get(Calendar.HOUR_OF_DAY) < 12) "Sáng" else "Chiều"
                        val label = String.format("%s %02d/%02d", session, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)

                        chartEntriesTemp.add(ChartEntry(label, record.temperature.toFloat()))
                        chartEntriesHumid.add(ChartEntry(label, record.humidity.toFloat()))

                        filteredRecords.add(record)
                        lastTimestamp = record.timestamp
                    }
                }
            }
        }
}
        // Cập nhật dữ liệu LiveData/StateFlow đẩy ra UI vẽ Chart
        _chartDataTemp.value = chartEntriesTemp
        _chartDataHumid.value = chartEntriesHumid

        // ── Tính thông số Max/Min/Avg dựa trên tập dữ liệu đã chọn lọc ────────────────────────────
        val temps = filteredRecords.map { it.temperature }
        val humids = filteredRecords.map { it.humidity }

        if (temps.isNotEmpty()) {
            _tempStats.value = SensorStats(
                min = temps.minOrNull() ?: 0.0,
                avg = temps.average(),
                max = temps.maxOrNull() ?: 0.0
            )
        }else {
            _tempStats.value= SensorStats(0.0,0.0,0.0)
        }
        if (humids.isNotEmpty()) {
            _humidStats.value = SensorStats(
                min = humids.minOrNull() ?: 0.0,
                avg = humids.average(),
                max = humids.maxOrNull() ?: 0.0
            )
        }
        else {
            _humidStats.value= SensorStats(0.0,0.0,0.0)
        }
    }



}

// ─────────────────────────────────────────────────────────────────────────
//  Enums & Data classes
// ─────────────────────────────────────────────────────────────────────────

enum class ChartFilter(val label: String) {
    TODAY("Hôm nay"),
    WEEK("7 ngày"),
    MONTH("30 ngày")
}

data class ChartEntry(val label: String, val value: Float)

data class SensorStats(
    val min: Double = 0.0,
    val avg: Double = 0.0,
    val max: Double = 0.0
)


data class HistoryRecord(
    val temperature: Double = 0.0,
    val humidity   : Double = 0.0,
    val timestamp  : Long   = 0L
)
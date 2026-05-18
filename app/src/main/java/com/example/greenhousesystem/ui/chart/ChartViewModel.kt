package com.example.greenhousesystem.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * ChartViewModel — Quản lý dữ liệu lịch sử cảm biến cho màn hình Chart.
 *
 * ✅ Đã loại bỏ hoàn toàn Mock Data.
 * ✅ Bổ sung StateFlow cho Empty State và Error Message.
 * ✅ Gom nhóm dữ liệu thật theo Giờ (Today) hoặc Ngày (Week/Month).
 */
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

    // ── Loading, Empty & Error States ─────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Trạng thái khi Firebase trả về danh sách rỗng (chưa có lịch sử)
    private val _isEmptyState = MutableStateFlow(false)
    val isEmptyState: StateFlow<Boolean> = _isEmptyState.asStateFlow()

    // Trạng thái lỗi (VD: Mất mạng, lỗi Firebase)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Threshold để vẽ LimitLine trên chart ─────────────────────────
    private val _tempThreshold = MutableStateFlow(Pair(15.0, 35.0))
    val tempThreshold: StateFlow<Pair<Double, Double>> = _tempThreshold.asStateFlow()

    private val _humidThreshold = MutableStateFlow(Pair(40.0, 90.0))
    val humidThreshold: StateFlow<Pair<Double, Double>> = _humidThreshold.asStateFlow()

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

    // ─────────────────────────────────────────────────────────────────
    //  fetchData — Lấy dữ liệu thật từ Firebase
    // ─────────────────────────────────────────────────────────────────
    fun fetchData(filter: ChartFilter = _currentFilter.value) {
        _isLoading.value = true
        _errorMessage.value = null // Reset lỗi mỗi lần tải lại

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val fromTime = when (filter) {
                    ChartFilter.TODAY -> now - 24L * 60 * 60 * 1000
                    ChartFilter.WEEK  -> now - 7L  * 24 * 60 * 60 * 1000
                    ChartFilter.MONTH -> now - 30L * 24 * 60 * 60 * 1000
                }

                // Lọc dữ liệu lớn hơn hoặc bằng fromTime
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
                    // Không có dữ liệu lịch sử trong khoảng thời gian này
                    _isEmptyState.value = true
                    processAndEmit(emptyList(), filter)
                } else {
                    // Có dữ liệu -> tiến hành vẽ
                    _isEmptyState.value = false
                    processAndEmit(rawList, filter)
                }
            } catch (e: Exception) {
                // Xử lý lỗi (Mất mạng, permission denied...)
                _errorMessage.value = "Lỗi kết nối dữ liệu: ${e.message}"
                _isEmptyState.value = false
                processAndEmit(emptyList(), filter)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  processAndEmit — Group theo giờ/ngày rồi tính stats
    // ─────────────────────────────────────────────────────────────────
    private fun processAndEmit(data: List<HistoryRecord>, filter: ChartFilter) {
        // Nếu không có dữ liệu, reset toàn bộ Chart và Stats về 0
        if (data.isEmpty()) {
            _chartDataTemp.value = emptyList()
            _chartDataHumid.value = emptyList()
            _tempStats.value = SensorStats()
            _humidStats.value = SensorStats()
            return
        }

        val cal = Calendar.getInstance()

        // Key group: giờ cho TODAY, ngày/tháng cho WEEK & MONTH
        val groupKey: (Long) -> String = { timestamp ->
            cal.timeInMillis = timestamp
            when (filter) {
                ChartFilter.TODAY ->
                    String.format("%02d:00", cal.get(Calendar.HOUR_OF_DAY))
                ChartFilter.WEEK, ChartFilter.MONTH ->
                    String.format(
                        "%02d/%02d",
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.MONTH) + 1
                    )
            }
        }

        // Sắp xếp theo thời gian → group → tính trung bình mỗi nhóm
        val grouped = data.sortedBy { it.timestamp }.groupBy { groupKey(it.timestamp) }

        _chartDataTemp.value = grouped.map { (label, items) ->
            ChartEntry(label = label, value = items.map { it.temperature }.average().toFloat())
        }
        _chartDataHumid.value = grouped.map { (label, items) ->
            ChartEntry(label = label, value = items.map { it.humidity }.average().toFloat())
        }

        // ── Tính stats min / avg / max ────────────────────────────
        val temps  = data.map { it.temperature }
        val humids = data.map { it.humidity }

        if (temps.isNotEmpty()) {
            _tempStats.value = SensorStats(
                min = temps.minOrNull() ?: 0.0,
                avg = temps.average(),
                max = temps.maxOrNull() ?: 0.0
            )
        }
        if (humids.isNotEmpty()) {
            _humidStats.value = SensorStats(
                min = humids.minOrNull() ?: 0.0,
                avg = humids.average(),
                max = humids.maxOrNull() ?: 0.0
            )
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
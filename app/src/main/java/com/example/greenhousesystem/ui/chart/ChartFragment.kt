package com.example.greenhousesystem.ui.chart

import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.greenhousesystem.databinding.FragmentChartBinding
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.model.SensorHistory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChartViewModel by viewModels()

    // Lưu timestamps để format trục X
    private var timestamps = listOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChartAppearance(binding.chartTemperature, "°C")
        setupChartAppearance(binding.chartHumidity, "%")
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupChartAppearance(chart: LineChart, unit: String) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            extraBottomOffset = 8f

            // Trục X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                textColor = Color.parseColor("#666666")
                textSize = 10f
                labelRotationAngle = -30f
                granularity = 1f
                // Format trục X theo HH:mm
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < timestamps.size) {
                            SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(timestamps[index]))
                        } else ""
                    }
                }
            }

            // Trục Y trái
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                textColor = Color.parseColor("#666666")
                textSize = 11f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}$unit"
                    }
                }
            }

            // Tắt trục Y phải
            axisRight.isEnabled = false

            // Animation
            animateX(800)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_green_dark
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHistory()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            if (!loading) binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.tvMockBadge.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        viewModel.historyData.observe(viewLifecycleOwner) { data ->
            if (data.isNotEmpty()) {
                timestamps = data.map { it.timestamp ?: 0L }
                updateTemperatureChart(data)
                updateHumidityChart(data)
                updateStats(data)
            }
        }

        viewModel.selectedPlant.observe(viewLifecycleOwner) { plant ->
            binding.tvTempThreshold.text =
                "Ngưỡng: ${plant.tempMin}°C ~ ${plant.tempMax}°C"
            binding.tvHumidThreshold.text =
                "Ngưỡng: ${plant.humidityMin}% ~ ${plant.humidityMax}%"

            // Cập nhật đường ngưỡng trên chart nếu đã có data
            viewModel.historyData.value?.let {
                if (it.isNotEmpty()) {
                    addLimitLines(binding.chartTemperature, plant.tempMin, plant.tempMax, "°C")
                    addLimitLines(binding.chartHumidity, plant.humidityMin, plant.humidityMax, "%")
                }
            }
        }
    }

    private fun updateTemperatureChart(data: List<SensorHistory>) {
        val entries = data.mapIndexed { index, sensor ->
            Entry(index.toFloat(), sensor.temperature?.toFloat() ?: 0f)
        }

        val dataSet = LineDataSet(entries, "Nhiệt độ").apply {
            color = Color.parseColor("#E53935")       // đỏ
            valueTextColor = Color.parseColor("#E53935")
            lineWidth = 2f
            circleRadius = 3f
            setCircleColor(Color.parseColor("#E53935"))
            setDrawCircleHole(false)
            setDrawValues(false)                       // ẩn value trên điểm
            mode = LineDataSet.Mode.CUBIC_BEZIER      // đường cong mượt
            setDrawFilled(true)
            fillColor = Color.parseColor("#E53935")
            fillAlpha = 30                             // fill nhạt phía dưới
        }

        binding.chartTemperature.data = LineData(dataSet)

        // Thêm đường ngưỡng nếu đã có plant
        viewModel.selectedPlant.value?.let { plant ->
            addLimitLines(binding.chartTemperature, plant.tempMin, plant.tempMax, "°C")
        }

        binding.chartTemperature.invalidate()
    }

    private fun updateHumidityChart(data: List<SensorHistory>) {
        val entries = data.mapIndexed { index, sensor ->
            Entry(index.toFloat(), sensor.humidity?.toFloat() ?: 0f)
        }

        val dataSet = LineDataSet(entries, "Độ ẩm").apply {
            color = Color.parseColor("#1E88E5")        // xanh dương
            valueTextColor = Color.parseColor("#1E88E5")
            lineWidth = 2f
            circleRadius = 3f
            setCircleColor(Color.parseColor("#1E88E5"))
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#1E88E5")
            fillAlpha = 30
        }

        binding.chartHumidity.data = LineData(dataSet)

        viewModel.selectedPlant.value?.let { plant ->
            addLimitLines(binding.chartHumidity, plant.humidityMin, plant.humidityMax, "%")
        }

        binding.chartHumidity.invalidate()
    }

    private fun addLimitLines(chart: LineChart, min: Double, max: Double, unit: String) {
        // Xóa limit lines cũ
        chart.axisLeft.removeAllLimitLines()

        // Đường ngưỡng MIN — xanh dương đứt
        val minLine = LimitLine(min.toFloat(), "Min ${min.toInt()}$unit").apply {
            lineWidth = 1.5f
            lineColor = Color.parseColor("#1565C0")
            enableDashedLine(10f, 8f, 0f)
            textColor = Color.parseColor("#1565C0")
            textSize = 10f
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        }

        // Đường ngưỡng MAX — đỏ đứt
        val maxLine = LimitLine(max.toFloat(), "Max ${max.toInt()}$unit").apply {
            lineWidth = 1.5f
            lineColor = Color.parseColor("#D32F2F")
            enableDashedLine(10f, 8f, 0f)
            textColor = Color.parseColor("#D32F2F")
            textSize = 10f
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        }

        chart.axisLeft.addLimitLine(minLine)
        chart.axisLeft.addLimitLine(maxLine)
        chart.invalidate()
    }

    private fun updateStats(data: List<SensorHistory>) {
        // Nhiệt độ
        val temps = data.mapNotNull { it.temperature }
        val minTemp = temps.minOrNull() ?: 0.0
        val maxTemp = temps.maxOrNull() ?: 0.0
        val avgTemp = if (temps.isNotEmpty()) temps.average() else 0.0

        binding.tvTempMin.text = String.format(Locale.getDefault(), "%.1f°C", minTemp)
        binding.tvTempMax.text = String.format(Locale.getDefault(), "%.1f°C", maxTemp)
        binding.tvTempAvg.text = String.format(Locale.getDefault(), "%.1f°C", avgTemp)

        // Độ ẩm
        val humids = data.mapNotNull { it.humidity }
        val minHumid = humids.minOrNull() ?: 0.0
        val maxHumid = humids.maxOrNull() ?: 0.0
        val avgHumid = if (humids.isNotEmpty()) humids.average() else 0.0

        binding.tvHumidMin.text = String.format(Locale.getDefault(), "%.1f%%", minHumid)
        binding.tvHumidMax.text = String.format(Locale.getDefault(), "%.1f%%", maxHumid)
        binding.tvHumidAvg.text = String.format(Locale.getDefault(), "%.1f%%", avgHumid)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
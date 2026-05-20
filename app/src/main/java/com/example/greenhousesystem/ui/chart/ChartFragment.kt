package com.example.greenhousesystem.ui.chart

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.greenhousesystem.databinding.FragmentChartBinding
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private val chartViewModel: ChartViewModel by viewModels()
    private val sharedViewModel: SharedDeviceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupFilterStrip()
        setupSwipeRefresh()
        observeViewModels()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SETUP CHARTS (Tối ưu hóa bảng màu Soft Sunset)
    // ─────────────────────────────────────────────────────────────────────
    private fun setupCharts() {
        listOf(binding.chartTemperature, binding.chartHumidity).forEach { chart ->
            chart.apply {
                setBackgroundColor(Color.TRANSPARENT)
                setDrawGridBackground(false)
                setDrawBorders(false)
                description.isEnabled = false
                legend.isEnabled      = false
                setTouchEnabled(true)
                isDragEnabled         = true
                setScaleEnabled(false)
                setPinchZoom(false)
                setNoDataText("Chưa có dữ liệu lịch sử...")
                setNoDataTextColor(Color.parseColor("#A29BFE")) // Màu Lavender nhã nhặn

                xAxis.apply {
                    position        = XAxis.XAxisPosition.BOTTOM
                    textColor       = Color.parseColor("#636E72") // Chữ xám dễ nhìn
                    textSize        = 9f
                    gridColor       = Color.parseColor("#33A29BFE") // Lưới Lavender trong suốt
                    gridLineWidth   = 0.5f
                    axisLineColor   = Color.parseColor("#CCFFFFFF") // Trục bắt sáng sáng sủa
                    setDrawAxisLine(true)
                    setDrawGridLines(true)
                    granularity     = 1f
                }

                axisLeft.apply {
                    textColor     = Color.parseColor("#636E72")
                    textSize      = 9f
                    gridColor     = Color.parseColor("#33A29BFE")
                    gridLineWidth = 0.5f
                    axisLineColor = Color.parseColor("#CCFFFFFF")
                }

                axisRight.isEnabled = false
                setExtraOffsets(8f, 16f, 8f, 8f)
            }
        }
    }

    private fun setupFilterStrip() {
        mapOf(
            binding.btnFilterToday to ChartFilter.TODAY,
            binding.btnFilterWeek  to ChartFilter.WEEK,
            binding.btnFilterMonth to ChartFilter.MONTH
        ).forEach { (btn, filter) ->
            btn.setOnClickListener { chartViewModel.setFilter(filter) }
        }
    }

    private fun updateFilterUi(activeFilter: ChartFilter) {
        val activeTextColor = Color.parseColor("#6C5CE7") // Lavender đậm
        val inactiveTextColor  = Color.parseColor("#636E72")

        mapOf(
            binding.btnFilterToday to ChartFilter.TODAY,
            binding.btnFilterWeek  to ChartFilter.WEEK,
            binding.btnFilterMonth to ChartFilter.MONTH
        ).forEach { (btn, filter) ->
            val isActive = filter == activeFilter
            btn.apply {
                setBackgroundResource(
                    if (isActive) com.example.greenhousesystem.R.drawable.bg_filter_active_light else 0
                )
                setTextColor(if (isActive) activeTextColor else inactiveTextColor)
                animate()
                    .scaleX(if (isActive) 1.05f else 1f)
                    .scaleY(if (isActive) 1.05f else 1f)
                    .setDuration(200L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(Color.parseColor("#A29BFE"))
            setProgressBackgroundColorSchemeColor(Color.parseColor("#FFFFFF"))
            setOnRefreshListener { chartViewModel.refresh() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE DATAFLOW từ Firebase và SharedViewModel
    // ─────────────────────────────────────────────────────────────────────
    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Loading State & Shimmer
                launch {
                    chartViewModel.isLoading.collectLatest { loading ->
                        binding.swipeRefresh.isRefreshing = loading
                        binding.shimmerTemp.apply {
                            if (loading) { visibility = View.VISIBLE; startShimmer() }
                            else { stopShimmer(); visibility = View.GONE }
                        }
                        binding.shimmerHumid.apply {
                            if (loading) { visibility = View.VISIBLE; startShimmer() }
                            else { stopShimmer(); visibility = View.GONE }
                        }

                        // Khi đang tải thì ẩn biểu đồ đi, tải xong mới hiện
                        binding.chartTemperature.visibility = if (loading) View.INVISIBLE else View.VISIBLE
                        binding.chartHumidity.visibility = if (loading) View.INVISIBLE else View.VISIBLE
                    }
                }

                // 2. Lắng nghe sự thay đổi của Filter để cập nhật UI Tab
                launch {
                    chartViewModel.currentFilter.collectLatest { filter ->
                        updateFilterUi(filter)
                    }
                }

                // 3. Trạng thái trống (Empty State) từ dữ liệu thật
                launch {
                    chartViewModel.isEmptyState.collectLatest { isEmpty ->
                        binding.tvMockBadge.apply {
                            visibility = if (isEmpty) View.VISIBLE else View.GONE
                            if (isEmpty) text = "Trống"
                        }
                    }
                }

                // 4. Hiển thị thông báo lỗi hệ thống/Firebase nếu có
                launch {
                    chartViewModel.errorMessage.collectLatest { msg ->
                        msg?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 5. Render biểu đồ Nhiệt độ dựa trên dữ liệu thật và Ngưỡng hiện tại
                launch {
                    chartViewModel.chartDataTemp.collectLatest { entries ->
                        if (entries.isNotEmpty()) {
                            renderChart(
                                chart = binding.chartTemperature,
                                entries = entries,
                                chartLineColor = Color.parseColor("#FF7675"), // Soft Pink đậm rực rỡ
                                chartFillColor = Color.parseColor("#26FAB1A0"), // Phủ nền lỏng trong suốt
                                thresholds = chartViewModel.tempThreshold.value
                            )
                        }
                    }
                }

                // 6. Render biểu đồ Độ ẩm dựa trên dữ liệu thật và Ngưỡng hiện tại
                launch {
                    chartViewModel.chartDataHumid.collectLatest { entries ->
                        if (entries.isNotEmpty()) {
                            renderChart(
                                chart = binding.chartHumidity,
                                entries = entries,
                                chartLineColor = Color.parseColor("#74B9FF"), // Soft Blue thanh thoát
                                chartFillColor = Color.parseColor("#2674B9FF"),
                                thresholds = chartViewModel.humidThreshold.value
                            )
                        }
                    }
                }

                // 7. Cập nhật dữ liệu Thống kê nhiệt độ (Min / Avg / Max) thật
                launch {
                    chartViewModel.tempStats.collectLatest { stats ->
                        binding.tvTempMin.text = String.format("%.1f°C", stats.min)
                        binding.tvTempAvg.text = String.format("%.1f°C", stats.avg)
                        binding.tvTempMax.text = String.format("%.1f°C", stats.max)
                    }
                }

                // 8. Cập nhật dữ liệu Thống kê độ ẩm (Min / Avg / Max) thật
                launch {
                    chartViewModel.humidStats.collectLatest { stats ->
                        binding.tvHumidMin.text = String.format("%.1f%%", stats.min)
                        binding.tvHumidAvg.text = String.format("%.1f%%", stats.avg)
                        binding.tvHumidMax.text = String.format("%.1f%%", stats.max)
                    }
                }

                // 9. Đồng bộ ngưỡng an toàn động từ SharedDeviceViewModel vào ChartViewModel
                launch {
                    sharedViewModel.thresholds.collectLatest { t ->
                        chartViewModel.setThresholds(t.tempMin, t.tempMax, t.humidMin, t.humidMax)
                        binding.tvTempThreshold.text = "Ngưỡng: ${t.tempMin.toInt()}°C ~ ${t.tempMax.toInt()}°C"
                        binding.tvHumidThreshold.text = "Ngưỡng: ${t.humidMin.toInt()}% ~ ${t.humidMax.toInt()}%"
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  RENDER CHART — Vẽ đường biểu diễn & các đường giới hạn cảnh báo
    // ─────────────────────────────────────────────────────────────────────
    private fun renderChart(
        chart: LineChart,
        entries: List<ChartEntry>,
        chartLineColor: Int,
        chartFillColor: Int,
        thresholds: Pair<Double, Double>
    ) {
        val mpEntries = entries.mapIndexed { i, e -> Entry(i.toFloat(), e.value) }

        val dataSet = LineDataSet(mpEntries, "").apply {
            color              = chartLineColor
            lineWidth          = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            mode               = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity     = 0.15f
            setDrawFilled(true)
            setFillColor(chartFillColor)
            fillAlpha          = 200
            highLightColor     = Color.parseColor("#A29BFE")
        }

        chart.data = LineData(dataSet)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(entries.map { it.label })
        chart.axisLeft.removeAllLimitLines()

        // ── Vẽ đường giới hạn dưới (Min Threshold Line) ──
        val minLine = LimitLine(thresholds.first.toFloat(), "Tối thiểu").apply {
            lineColor     = Color.parseColor("#74B9FF")
            lineWidth     = 1f
            enableDashedLine(10f, 6f, 0f)
            textColor     = Color.parseColor("#636E72")
            textSize      = 8f
            labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
        }
        chart.axisLeft.addLimitLine(minLine)

        // ── Vẽ đường giới hạn trên (Max Threshold Line) ──
        val maxLine = LimitLine(thresholds.second.toFloat(), "Tối đa").apply {
            lineColor     = Color.parseColor("#FF7675")
            lineWidth     = 1f
            enableDashedLine(10f, 6f, 0f)
            textColor     = Color.parseColor("#FF7675")
            textSize      = 8f
            labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
        }
        chart.axisLeft.addLimitLine(maxLine)

        chart.animateX(1000)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.greenhousesystem.ui.chart

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
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

    private var tempLabels  = listOf<String>()
    private var humidLabels = listOf<String>()

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
    //  SETUP CHARTS
    // ─────────────────────────────────────────────────────────────────────
    private fun setupCharts() {
        listOf(binding.chartTemperature, binding.chartHumidity).forEach { chart ->
            chart.apply {
                setBackgroundColor(Color.TRANSPARENT)
                description.isEnabled = false
                legend.isEnabled      = false
                setTouchEnabled(true)
                isDragEnabled         = true
                setScaleEnabled(false)
                setPinchZoom(false)
                setDrawGridBackground(false)
                setNoDataText("Đang tải dữ liệu...")
                setNoDataTextColor(Color.parseColor("#4A8C52"))

                xAxis.apply {
                    position        = XAxis.XAxisPosition.BOTTOM
                    textColor       = Color.parseColor("#4A8C52")
                    textSize        = 9f
                    gridColor       = Color.parseColor("#1A2D1E")
                    gridLineWidth   = 0.5f
                    axisLineColor   = Color.parseColor("#2D4A31")
                    setDrawAxisLine(true)
                    setDrawGridLines(true)
                    granularity     = 1f
                    labelCount      = 6
                }

                axisLeft.apply {
                    textColor     = Color.parseColor("#4A8C52")
                    textSize      = 9f
                    gridColor     = Color.parseColor("#1A2D1E")
                    gridLineWidth = 0.5f
                    axisLineColor = Color.parseColor("#2D4A31")
                }

                axisRight.isEnabled = false
                setExtraOffsets(8f, 16f, 8f, 8f)
                animateX(1200)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  FILTER STRIP
    // ─────────────────────────────────────────────────────────────────────
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
        val limeText = Color.parseColor("#84CC16")
        val dimText  = Color.parseColor("#4A6A4E")

        mapOf(
            binding.btnFilterToday to ChartFilter.TODAY,
            binding.btnFilterWeek  to ChartFilter.WEEK,
            binding.btnFilterMonth to ChartFilter.MONTH
        ).forEach { (btn, filter) ->
            val isActive = filter == activeFilter
            btn.apply {
                setBackgroundColor(
                    if (isActive) Color.parseColor("#1584CC16") else Color.TRANSPARENT
                )
                setTextColor(if (isActive) limeText else dimText)
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
            setColorSchemeColors(Color.parseColor("#84CC16"))
            setProgressBackgroundColorSchemeColor(Color.parseColor("#0F1E12"))
            setOnRefreshListener { chartViewModel.refresh() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE
    // ─────────────────────────────────────────────────────────────────────
    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Loading → Shimmer
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
                        binding.chartTemperature.visibility =
                            if (loading) View.INVISIBLE else View.VISIBLE
                        binding.chartHumidity.visibility =
                            if (loading) View.INVISIBLE else View.VISIBLE
                    }
                }

                // Filter tab
                launch {
                    chartViewModel.currentFilter.collectLatest { updateFilterUi(it) }
                }

                // Mock badge
                launch {
                    chartViewModel.isMockData.collectLatest { isMock ->
                        binding.tvMockBadge.visibility =
                            if (isMock) View.VISIBLE else View.GONE
                    }
                }

                // Temperature chart
                launch {
                    chartViewModel.chartDataTemp.collectLatest { entries ->
                        if (entries.isNotEmpty()) {
                            renderChart(
                                chart      = binding.chartTemperature,
                                entries    = entries,
                                // ✅ FIX: đổi tên param thành chartLineColor / chartFillColor
                                // tránh bị Kotlin hiểu nhầm là property của LineDataSet
                                // khi gọi trong scope apply{} bên trong renderChart.
                                chartLineColor = Color.parseColor("#EF5350"),
                                chartFillColor = Color.parseColor("#1AEF5350"),
                                thresholds     = chartViewModel.tempThreshold.value
                            )
                        }
                        tempLabels = entries.map { it.label }
                    }
                }

                // Humidity chart
                launch {
                    chartViewModel.chartDataHumid.collectLatest { entries ->
                        if (entries.isNotEmpty()) {
                            renderChart(
                                chart          = binding.chartHumidity,
                                entries        = entries,
                                chartLineColor = Color.parseColor("#42A5F5"),
                                chartFillColor = Color.parseColor("#1A42A5F5"),
                                thresholds     = chartViewModel.humidThreshold.value
                            )
                        }
                        humidLabels = entries.map { it.label }
                    }
                }

                // Stats nhiệt độ
                launch {
                    chartViewModel.tempStats.collectLatest { stats ->
                        binding.tvTempMin.text = String.format("%.1f°C", stats.min)
                        binding.tvTempAvg.text = String.format("%.1f°C", stats.avg)
                        binding.tvTempMax.text = String.format("%.1f°C", stats.max)
                    }
                }

                // Stats độ ẩm
                launch {
                    chartViewModel.humidStats.collectLatest { stats ->
                        binding.tvHumidMin.text = String.format("%.1f%%", stats.min)
                        binding.tvHumidAvg.text = String.format("%.1f%%", stats.avg)
                        binding.tvHumidMax.text = String.format("%.1f%%", stats.max)
                    }
                }

                // Threshold từ SharedDeviceViewModel → limit lines
                launch {
                    sharedViewModel.thresholds.collectLatest { t ->
                        chartViewModel.setThresholds(
                            t.tempMin, t.tempMax, t.humidMin, t.humidMax
                        )
                        binding.tvTempThreshold.text =
                            "Ngưỡng: ${t.tempMin.toInt()}°C ~ ${t.tempMax.toInt()}°C"
                        binding.tvHumidThreshold.text =
                            "Ngưỡng: ${t.humidMin.toInt()}% ~ ${t.humidMax.toInt()}%"
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  RENDER CHART
    //
    //  ✅ FIX (line 305, 320, 331): Đổi tên tham số từ lineColor/fillColor
    //  thành chartLineColor/chartFillColor.
    //
    //  Nguyên nhân lỗi "val cannot be reassigned":
    //  Bên trong LineDataSet.apply{ fillColor = fillColor } — Kotlin hiểu
    //  vế phải "fillColor" là property của LineDataSet (Int), không phải
    //  tham số hàm (Int) → gán Int cho Int nhưng báo "val cannot be reassigned"
    //  vì fillColor của LineDataSet là val trong một số version MPAndroidChart.
    //
    //  Tương tự với lineColor trong LimitLine.apply{ lineColor = lineColor }.
    //
    //  Giải pháp: đổi tên param hàm để không shadow property của DataSet.
    // ─────────────────────────────────────────────────────────────────────
    private fun renderChart(
        chart          : LineChart,
        entries        : List<ChartEntry>,
        chartLineColor : Int,          // ✅ tên mới, không conflict với LineDataSet.color
        chartFillColor : Int,          // ✅ tên mới, không conflict với LineDataSet.fillColor
        thresholds     : Pair<Double, Double>
    ) {
        val mpEntries = entries.mapIndexed { i, e -> Entry(i.toFloat(), e.value) }

        val dataSet = LineDataSet(mpEntries, "").apply {
            // ✅ Dùng chartLineColor (tham số hàm), không phải property của LineDataSet
            color              = chartLineColor
            lineWidth          = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            mode               = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity     = 0.2f
            setDrawFilled(true)
            // ✅ Gán trực tiếp bằng setter để rõ ràng
            setFillColor(chartFillColor)
            fillAlpha          = 180
            highLightColor     = Color.parseColor("#84CC16")
        }

        chart.data = LineData(dataSet)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(entries.map { it.label })
        chart.axisLeft.removeAllLimitLines()

        // ── LimitLine ngưỡng MIN ──────────────────────────────────────
        // ✅ FIX: Dùng biến local minLimitLine thay vì apply{} để tránh
        // shadow property lineColor của LimitLine.
        val minLine = LimitLine(thresholds.first.toFloat(), "Min")
        minLine.lineColor    = Color.parseColor("#1565C0")  // ✅ rõ ràng, không ambiguous
        minLine.lineWidth    = 1f
        minLine.enableDashedLine(10f, 5f, 0f)
        minLine.textColor    = Color.parseColor("#64B5F6")
        minLine.textSize     = 9f
        minLine.labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
        chart.axisLeft.addLimitLine(minLine)

        // ── LimitLine ngưỡng MAX ──────────────────────────────────────
        val maxLine = LimitLine(thresholds.second.toFloat(), "Max")
        maxLine.lineColor    = Color.parseColor("#C62828")  // ✅ rõ ràng
        maxLine.lineWidth    = 1f
        maxLine.enableDashedLine(10f, 5f, 0f)
        maxLine.textColor    = Color.parseColor("#EF9A9A")
        maxLine.textSize     = 9f
        maxLine.labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
        chart.axisLeft.addLimitLine(maxLine)

        chart.animateX(1200)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
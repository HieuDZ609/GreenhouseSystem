package com.example.greenhousesystem.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.greenhousesystem.databinding.FragmentHomeBinding
import com.example.greenhousesystem.model.LedMode
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.model.SensorData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    // Tránh trigger listener khi set giá trị programmatically
    private var isUpdatingSwitch = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupSwipeRefresh()
        setupLedSwitch()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_green_dark
        )
        // Firebase realtime nên chỉ cần tắt loading sau 1s
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.postDelayed({
                binding.swipeRefresh.isRefreshing = false
            }, 1000)
        }
    }

    private fun setupLedSwitch() {
        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                viewModel.toggleLed(!isChecked) // toggle ngược lại
            }
        }
    }

    private fun observeViewModel() {
        // Sensor data
        viewModel.sensorData.observe(viewLifecycleOwner) { data ->
            updateSensorUI(data)
        }

        // Device status
        viewModel.deviceStatus.observe(viewLifecycleOwner) { status ->
            val isOnline = status.status == "online"
            binding.viewStatusDot.setBackgroundResource(
                if (isOnline) com.example.greenhousesystem.R.drawable.circle_green
                else com.example.greenhousesystem.R.drawable.circle_gray
            )
            binding.tvDeviceStatus.text = if (isOnline) "Online" else "Offline"
            binding.tvDeviceStatus.setTextColor(
                if (isOnline) Color.parseColor("#2E7D32")
                else Color.parseColor("#9E9E9E")
            )
            binding.tvLastSeen.text = if (status.lastSeen > 0)
                "Lần cuối: ${formatTime(status.lastSeen)}" else ""
        }

        // LED status
        viewModel.ledStatus.observe(viewLifecycleOwner) { led ->
            isUpdatingSwitch = true
            binding.switchLed.isChecked = led.isOn
            isUpdatingSwitch = false

            // Hiển thị mode
            val mode = try {
                LedMode.valueOf(led.mode)
            } catch (e: Exception) { LedMode.MANUAL }

            binding.tvLedMode.text = "${mode.icon} ${mode.displayName}"

            // Màu LED preview
            val color = Color.rgb(led.red, led.green, led.blue)
            binding.viewLedColor.setBackgroundColor(color)
        }

        // Plant
        viewModel.selectedPlant.observe(viewLifecycleOwner) { plant ->
            binding.tvPlantName.text = "${plant.icon} ${plant.name}"
            // Re-check alert với plant mới
        }

        // Alert banner
        viewModel.alertMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                binding.cardAlert.visibility = View.VISIBLE
                binding.tvAlertMessage.text = message
            } else {
                binding.cardAlert.visibility = View.GONE
            }
        }
    }

    private fun updateSensorUI(data: SensorData) {
        val plant = viewModel.selectedPlant.value

        // Nhiệt độ
        binding.tvTemperature.text = String.format("%.1f°C", data.temperature)
        val tempState = getTempState(data.temperature, plant)
        binding.tvTempStatus.text = tempState.label
        binding.tvTempStatus.setTextColor(Color.parseColor(tempState.color))
        binding.cardTemperature.setCardBackgroundColor(Color.parseColor(tempState.bgColor))

        // Độ ẩm
        binding.tvHumidity.text = String.format("%.1f%%", data.humidity)
        val humidState = getHumidState(data.humidity, plant)
        binding.tvHumidityStatus.text = humidState.label
        binding.tvHumidityStatus.setTextColor(Color.parseColor(humidState.color))
        binding.cardHumidity.setCardBackgroundColor(Color.parseColor(humidState.bgColor))

        // Last update
        binding.tvLastUpdate.text = "Cập nhật lần cuối: ${formatTime(data.timestamp)}"
    }

    private fun getTempState(temp: Double, plant: PlantProfile?): SensorState {
        if (plant == null) return SensorState("Đang tải...", "#9E9E9E", "#FFFFFF")
        return when {
            temp > plant.tempMax -> SensorState("⚠️ Quá cao!", "#D32F2F", "#FFEBEE")
            temp < plant.tempMin -> SensorState("⚠️ Quá thấp!", "#1565C0", "#E3F2FD")
            temp > plant.tempMax - 2 -> SensorState("⚡ Gần ngưỡng cao", "#F57F17", "#FFFDE7")
            else -> SensorState("✅ Bình thường", "#2E7D32", "#F1F8E9")
        }
    }

    private fun getHumidState(humidity: Double, plant: PlantProfile?): SensorState {
        if (plant == null) return SensorState("Đang tải...", "#9E9E9E", "#FFFFFF")
        return when {
            humidity > plant.humidityMax -> SensorState("⚠️ Quá cao!", "#D32F2F", "#FFEBEE")
            humidity < plant.humidityMin -> SensorState("⚠️ Quá thấp!", "#1565C0", "#E3F2FD")
            humidity > plant.humidityMax - 5 -> SensorState("⚡ Gần ngưỡng cao", "#F57F17", "#FFFDE7")
            else -> SensorState("✅ Bình thường", "#2E7D32", "#F1F8E9")
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "--"
        return SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            .format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Helper data class
data class SensorState(
    val label: String,
    val color: String,
    val bgColor: String
)
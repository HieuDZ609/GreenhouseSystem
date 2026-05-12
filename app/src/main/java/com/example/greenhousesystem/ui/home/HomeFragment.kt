package com.example.greenhousesystem.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentHomeBinding
import com.example.greenhousesystem.model.LedMode
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.ui.animation.AnimationHelper
import com.example.greenhousesystem.ui.animation.AnimationHelper.setupSpringPress
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════
//  HomeFragment — "Smart Oasis" Dashboard chính
//
//  ✅ FIX:
//  1. Xóa HomeViewModel riêng — tất cả data đọc từ SharedDeviceViewModel
//     (activityViewModels) bằng StateFlow + repeatOnLifecycle.
//  2. Xóa sharedViewModel.selectedDevice — field không tồn tại.
//  3. Xóa viewModel.loadDeviceData() / toggleLed(deviceId, ...) — không tồn tại.
//  4. Xóa AnimationHelper.postWithAnimation — không tồn tại.
//  5. LED toggle gọi sharedViewModel.toggleLed(isOn) trực tiếp.
// ═══════════════════════════════════════════════════════════
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ✅ Chỉ dùng SharedDeviceViewModel — single source of truth
    private val sharedViewModel: SharedDeviceViewModel by activityViewModels()

    private var isUpdatingSwitch = false
    private var wasOnline        = false

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
        setupEntrance()
        setupSwipeRefresh()
        setupLedSwitch()
        setupSpringButtons()
        // ✅ Chỉ gọi observeViewModel() 1 lần (file gốc gọi 2 lần)
        observeViewModel()
    }

    // ─────────────────────────────────────────────────────────
    //  STAGGERED ENTRANCE
    // ─────────────────────────────────────────────────────────
    private fun setupEntrance() {
        val viewsToAnimate = listOf(
            binding.layoutHeader,
            binding.cardDeviceStatus,
            binding.cardTemperature,
            binding.cardHumidity,
            binding.cardLed,
            binding.tvLastUpdate
        )
        AnimationHelper.staggerEntrance(viewsToAnimate, baseDelay = 80L, duration = 450L)
    }

    private fun setupSpringButtons() {
        binding.btnNotification.setupSpringPress()
        binding.switchLed.setupSpringPress()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#84CC16"))
        binding.swipeRefresh.setOnRefreshListener {
            // ✅ Firebase realtime tự cập nhật, chỉ cần tắt indicator sau 1s
            binding.swipeRefresh.postDelayed(
                { binding.swipeRefresh.isRefreshing = false },
                1000L
            )
        }
    }

    private fun setupLedSwitch() {
        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                // ✅ Gọi SharedDeviceViewModel.toggleLed() — tồn tại và đúng signature
                sharedViewModel.toggleLed(isChecked)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  OBSERVE — Dùng StateFlow + repeatOnLifecycle
    //
    //  ✅ Không dùng LiveData .observe() nữa vì SharedDeviceViewModel
    //  expose StateFlow. repeatOnLifecycle(STARTED) tự pause/resume
    //  an toàn theo lifecycle của Fragment.
    // ─────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Sensor data (nhiệt độ + độ ẩm)
                launch {
                    sharedViewModel.sensorData.collectLatest { data ->
                        updateSensorUi(
                            temp      = data.temperature,
                            humid     = data.humidity,
                            timestamp = data.timestamp
                        )
                    }
                }

                // Device status (online / offline)
                launch {
                    sharedViewModel.deviceStatus.collectLatest { status ->
                        updateDeviceStatusUi(
                            isOnline = status.status == "online",
                            lastSeen = status.lastSeen
                        )
                    }
                }

                // LED status
                launch {
                    sharedViewModel.ledStatus.collectLatest { led ->
                        updateLedUi(led.isOn, led.red, led.green, led.blue, led.mode)
                    }
                }

                // Cây đang chọn
                launch {
                    sharedViewModel.selectedPlant.collectLatest { plant ->
                        plant ?: return@collectLatest
                        binding.tvPlantName.text = "${plant.icon} ${plant.name}"
                    }
                }

                // Alert message
                launch {
                    sharedViewModel.alertMessage.collectLatest { message ->
                        if (message != null) {
                            binding.tvAlertMessage.text = message
                            if (binding.cardAlert.visibility != View.VISIBLE) {
                                AnimationHelper.showAlert(
                                    binding.cardAlert,
                                    binding.iconAlertContainer
                                )
                            }
                        } else {
                            if (binding.cardAlert.visibility == View.VISIBLE) {
                                AnimationHelper.hideAlert(binding.cardAlert)
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE SENSOR UI
    // ─────────────────────────────────────────────────────────
    private fun updateSensorUi(temp: Double, humid: Double, timestamp: Long) {
        // Lấy plant từ StateFlow hiện tại (không cần LiveData)
        val plant = sharedViewModel.selectedPlant.value

        // ── Nhiệt độ ─────────────────────────────────────────
        binding.gaugeTemperature.setValue(temp.toFloat())
        AnimationHelper.animateCounter(binding.tvTemperature, temp.toFloat(), "°C")

        val tempState = getTempState(temp, plant)
        binding.tvTempStatus.text = tempState.label
        binding.tvTempStatus.setTextColor(Color.parseColor(tempState.color))
        binding.cardTemperature.setCardBackgroundColor(Color.parseColor(tempState.cardBg))

        // ── Độ ẩm ─────────────────────────────────────────────
        binding.gaugeHumidity.setValue(humid.toFloat())
        AnimationHelper.animateCounter(binding.tvHumidity, humid.toFloat(), "%")

        val humidState = getHumidState(humid, plant)
        binding.tvHumidityStatus.text = humidState.label
        binding.tvHumidityStatus.setTextColor(Color.parseColor(humidState.color))
        binding.cardHumidity.setCardBackgroundColor(Color.parseColor(humidState.cardBg))

        binding.tvLastUpdate.text = "Cập nhật: ${formatTime(timestamp)}"
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE DEVICE STATUS UI
    // ─────────────────────────────────────────────────────────
    private fun updateDeviceStatusUi(isOnline: Boolean, lastSeen: Long) {
        if (isOnline) {
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_green)
            binding.tvDeviceStatus.text = "Online"
            binding.tvDeviceStatus.setTextColor(Color.parseColor("#84CC16"))
            binding.tvLastSeen.text = ""

            if (!wasOnline) {
                AnimationHelper.startPulse(binding.viewPulseOuter, binding.viewPulseMiddle)
                AnimationHelper.applyOfflineFilter(binding.root, false)
            }
            wasOnline = true
        } else {
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_gray)
            binding.tvDeviceStatus.text = "Offline"
            binding.tvDeviceStatus.setTextColor(Color.parseColor("#4A5A4E"))
            if (lastSeen > 0) {
                binding.tvLastSeen.text = "Lần cuối: ${formatTime(lastSeen)}"
            }

            if (wasOnline) {
                AnimationHelper.stopPulse(binding.viewPulseOuter, binding.viewPulseMiddle)
                AnimationHelper.applyOfflineFilter(binding.root, true)
            }
            wasOnline = false
        }
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE LED UI
    // ─────────────────────────────────────────────────────────
    private fun updateLedUi(isOn: Boolean, r: Int, g: Int, b: Int, modeStr: String) {
        isUpdatingSwitch = true
        binding.switchLed.isChecked = isOn
        isUpdatingSwitch = false

        val ledColor = Color.rgb(r, g, b)

        binding.viewLedColor.animate()
            .alpha(0f)
            .setDuration(150L)
            .withEndAction {
                binding.viewLedColor.setBackgroundColor(ledColor)
                binding.viewLedColor.animate().alpha(1f).setDuration(150L).start()
            }
            .start()

        binding.viewLedGlow.strokeColor =
            if (isOn) ledColor else Color.parseColor("#1A2A1E")

        val mode = try { LedMode.valueOf(modeStr) } catch (e: Exception) { LedMode.MANUAL }
        binding.tvLedMode.text = "${mode.icon} ${mode.displayName}"
        binding.tvLedRgb.text  = "R:$r  G:$g  B:$b"
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS — Trạng thái sensor theo ngưỡng cây
    // ─────────────────────────────────────────────────────────
    private fun getTempState(temp: Double, plant: PlantProfile?): SensorState {
        if (plant == null) return SensorState("Đang tải...", "#4A8C52", "#0F1E12")
        return when {
            temp > plant.tempMax         -> SensorState("⚠️ Quá cao!",       "#FF6B6B", "#2D0A0A")
            temp < plant.tempMin         -> SensorState("⚠️ Quá thấp!",      "#64B5F6", "#0A1520")
            temp > plant.tempMax - 2     -> SensorState("⚡ Gần ngưỡng cao", "#FFB74D", "#2A1A08")
            else                         -> SensorState("✅ Bình thường",    "#84CC16", "#0F1E12")
        }
    }

    private fun getHumidState(humidity: Double, plant: PlantProfile?): SensorState {
        if (plant == null) return SensorState("Đang tải...", "#4A70A8", "#0D1A2A")
        return when {
            humidity > plant.humidityMax     -> SensorState("⚠️ Quá cao!",       "#FF6B6B", "#1A0A0D")
            humidity < plant.humidityMin     -> SensorState("⚠️ Quá thấp!",      "#64B5F6", "#0A1525")
            humidity > plant.humidityMax - 5 -> SensorState("⚡ Gần ngưỡng cao", "#FFB74D", "#1A1208")
            else                             -> SensorState("✅ Bình thường",    "#42A5F5", "#0D1A2A")
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "--"
        return SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─────────────────────────────────────────────────────────────────────────
data class SensorState(
    val label : String,
    val color : String,
    val cardBg: String
)
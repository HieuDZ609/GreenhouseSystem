package com.example.greenhousesystem.ui.home

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentHomeBinding
import com.example.greenhousesystem.model.LedMode
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import com.example.greenhousesystem.ui.animation.AnimationHelper
import com.example.greenhousesystem.ui.animation.AnimationHelper.setupSpringPress
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════
//  HomeFragment — "Smart Oasis" Dashboard (Soft Sunset Theme)
//  Tích hợp Glassmorphism, RenderEffect Blur và Visual Color Coding.
// ═══════════════════════════════════════════════════════════
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Sử dụng chung ViewModel với Activity để đồng bộ dữ liệu
    private val sharedViewModel: SharedDeviceViewModel by activityViewModels()

    private var isUpdatingSwitch = false
    private var wasOnline = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyGlassBlur() // Bật hiệu ứng mờ kính cho các Card
        setupEntrance()
        setupSwipeRefresh()
        setupLedSwitch()
        setupSpringButtons()
        observeViewModel()
    }

    // ─────────────────────────────────────────────────────────
    //  GLASSMORPHISM & UI SETUP
    // ─────────────────────────────────────────────────────────
    private fun applyGlassBlur() {
        // Chỉ áp dụng Blur cho Android 12 trở lên (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blur = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
            binding.cardTemperature.setRenderEffect(blur)
            binding.cardHumidity.setRenderEffect(blur)
            binding.cardLed.setRenderEffect(blur)
            binding.cardDeviceStatus.setRenderEffect(blur)
        }
    }

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
        binding.btnThemeToggle.setupSpringPress()
    }

    private fun setupSwipeRefresh() {
        // Màu vòng xoay (Soft green)
        binding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.status_green_soft))
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.postDelayed({ binding.swipeRefresh.isRefreshing = false }, 1000L)
        }
    }

    private fun setupLedSwitch() {
        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                sharedViewModel.toggleLed(isChecked)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  OBSERVE VIEWMODEL (STATE FLOW)
    // ─────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Lắng nghe dữ liệu cảm biến
                launch {
                    sharedViewModel.sensorData.collectLatest { data ->
                        updateSensorUi(data.temperature, data.humidity, data.timestamp)
                    }
                }

                // Lắng nghe trạng thái thiết bị
                launch {
                    sharedViewModel.deviceStatus.collectLatest { status ->
                        updateDeviceStatusUi(status.status == "online", status.lastSeen)
                    }
                }

                // Lắng nghe trạng thái đèn LED
                launch {
                    sharedViewModel.ledStatus.collectLatest { led ->
                        updateLedUi(led.isOn, led.red, led.green, led.blue, led.mode)
                    }
                }

                // Lắng nghe cấu hình cây
                launch {
                    sharedViewModel.selectedPlant.collectLatest { plant ->
                        plant ?: return@collectLatest
                        binding.tvPlantName.text = "${plant.icon} ${plant.name}"
                    }
                }

                // Lắng nghe thông báo lỗi/vượt ngưỡng
                launch {
                    sharedViewModel.alertMessage.collectLatest { message ->
                        if (message != null) {
                            binding.tvAlertMessage.text = message
                            if (binding.cardAlert.visibility != View.VISIBLE) {
                                AnimationHelper.showAlert(binding.cardAlert, binding.iconAlertContainer)
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
    //  CẬP NHẬT GIAO DIỆN SENSOR (GIỮ HIỆU ỨNG KÍNH)
    // ─────────────────────────────────────────────────────────
    private fun updateSensorUi(temp: Double, humid: Double, timestamp: Long) {
        val plant = sharedViewModel.selectedPlant.value

        // 1. Cập nhật Nhiệt độ
        binding.gaugeTemperature.setValue(temp.toFloat())
        AnimationHelper.animateCounter(binding.tvTemperature, temp.toFloat(), "°C")

        val tempState = getTempState(temp, plant)
        val tempColor = ContextCompat.getColor(requireContext(), tempState.colorRes)

        binding.tvTempStatus.text = tempState.label
        binding.tvTempStatus.setTextColor(tempColor)
        // Thay đổi viền card thay vì đổi nền để giữ hiệu ứng kính
        binding.cardTemperature.strokeColor = tempColor

        // 2. Cập nhật Độ ẩm
        binding.gaugeHumidity.setValue(humid.toFloat())
        AnimationHelper.animateCounter(binding.tvHumidity, humid.toFloat(), "%")

        val humidState = getHumidState(humid, plant)
        val humidColor = ContextCompat.getColor(requireContext(), humidState.colorRes)

        binding.tvHumidityStatus.text = humidState.label
        binding.tvHumidityStatus.setTextColor(humidColor)
        binding.cardHumidity.strokeColor = humidColor

        // 3. Thời gian
        binding.tvLastUpdate.text = "Cập nhật: ${formatTime(timestamp)}"
    }

    // ─────────────────────────────────────────────────────────
    //  CẬP NHẬT TRẠNG THÁI THIẾT BỊ
    // ─────────────────────────────────────────────────────────
    private fun updateDeviceStatusUi(isOnline: Boolean, lastSeen: Long) {
        if (isOnline) {
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_green)
            binding.tvDeviceStatus.text = "Online"
            binding.tvDeviceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green_soft))

            if (!wasOnline) {
                AnimationHelper.startPulse(binding.viewPulseOuter, binding.viewPulseMiddle)
                AnimationHelper.applyOfflineFilter(binding.root, false)
            }
            wasOnline = true
        } else {
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_gray)
            binding.tvDeviceStatus.text = "Offline"
            binding.tvDeviceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.charcoal_light))

            if (wasOnline) {
                AnimationHelper.stopPulse(binding.viewPulseOuter, binding.viewPulseMiddle)
                AnimationHelper.applyOfflineFilter(binding.root, true)
            }
            wasOnline = false
        }
    }

    // ─────────────────────────────────────────────────────────
    //  CẬP NHẬT GIAO DIỆN LED
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

        // Card LED glow nhẹ theo màu đèn
        binding.viewLedGlow.strokeColor = if (isOn) ledColor else ContextCompat.getColor(requireContext(), R.color.glass_stroke_soft)

        val mode = try { LedMode.valueOf(modeStr) } catch (e: Exception) { LedMode.MANUAL }
        binding.tvLedMode.text = "${mode.icon} ${mode.displayName}"
        binding.tvLedRgb.text = "R:$r  G:$g  B:$b"
    }

    // ─────────────────────────────────────────────────────────
    //  HÀM PHỤ TRỢ (HELPER)
    // ─────────────────────────────────────────────────────────
    private fun getTempState(temp: Double, plant: PlantProfile?): SensorState {
        if (plant == null) return SensorState("Đang tải...", R.color.text_hint_light)
        return when {
            temp > plant.tempMax -> SensorState("⚠️ Quá cao", R.color.status_red_soft)
            temp < plant.tempMin -> SensorState("⚠️ Quá thấp", R.color.status_blue_soft)
            temp > plant.tempMax - 2 -> SensorState("⚡ Gần ngưỡng", R.color.status_orange_soft)
            else -> SensorState("✅ Bình thường", R.color.status_green_soft)
        }
    }

    private fun getHumidState(humidity: Double, plant: PlantProfile?): SensorState {
        if (plant == null) return SensorState("Đang tải...", R.color.text_hint_light)
        return when {
            humidity > plant.humidityMax -> SensorState("⚠️ Quá cao", R.color.status_red_soft)
            humidity < plant.humidityMin -> SensorState("⚠️ Quá thấp", R.color.status_blue_soft)
            humidity > plant.humidityMax - 5 -> SensorState("⚡ Gần ngưỡng", R.color.status_orange_soft)
            else -> SensorState("✅ Bình thường", R.color.status_green_soft)
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
// Data class UI dùng Resource ID (Tối ưu UI/UX)
data class SensorState(
    val label: String,
    val colorRes: Int
)
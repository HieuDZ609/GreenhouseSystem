package com.example.greenhousesystem.ui.drawer

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentThresholdBinding
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ThresholdFragment — Cài đặt ngưỡng cảnh báo nhiệt độ và độ ẩm.
 *
 * Kết nối với SharedDeviceViewModel (activityViewModels):
 * - Đọc threshold hiện tại từ StateFlow _thresholds để khởi tạo slider
 * - Gọi sharedViewModel.saveThresholds() khi user nhấn Lưu
 * - Đọc selectedPlant để hiển thị tên cây + giá trị mặc định
 *
 * RangeSlider (Material 3):
 * - binding.sliderTempRange.values = listOf(min, max) để set 2 đầu
 * - addOnChangeListener nhận List<Float> = [currentMin, currentMax]
 * - Validation: currentMin < currentMax → màu chữ đổi sang đỏ nếu fail
 *
 * Live Range Preview Bar:
 * - viewTempRangeBar width thay đổi theo (max - min) / totalRange
 * - viewTempRangeBar marginStart thay đổi theo min / totalRange
 * - Cần ViewTreeObserver để lấy width thật của track container
 *
 * Save flow:
 * Nhấn Lưu → validate → disable button → sharedViewModel.saveThresholds()
 * → onSuccess: bounce animation + Snackbar xanh
 * → onError: re-enable button + Snackbar đỏ
 *
 * Lưu vào Firebase path: GreenHouseSystem/users/$uid/customThresholds
 * (AlertWorker đọc từ path này khi kiểm tra ngưỡng)
 */
class ThresholdFragment : Fragment() {

    private var _binding: FragmentThresholdBinding? = null
    private val binding get() = _binding!!

    // activityViewModels() → cùng instance với MainActivity
    private val sharedViewModel: SharedDeviceViewModel by activityViewModels()

    // Lưu giá trị default của plant để nút "Đặt lại" reset về
    private var plantTempMin  = 15f
    private var plantTempMax  = 35f
    private var plantHumidMin = 40f
    private var plantHumidMax = 90f

    // Flag tránh vòng lặp khi set slider value từ code
    private var isInitializingSliders = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThresholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRangeSliders()
        setupClickListeners()
        observeSharedViewModel()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SETUP RANGE SLIDERS — Khởi tạo listener cho RangeSlider
    //
    //  RangeSlider.addOnChangeListener nhận:
    //  slider: RangeSlider, values: List<Float>, fromUser: Boolean
    //  values[0] = min, values[1] = max
    // ─────────────────────────────────────────────────────────────────────
    private fun setupRangeSliders() {
        // Listener cho slider nhiệt độ
        binding.sliderTempRange.addOnChangeListener { _, _, _ ->
            if (!isInitializingSliders) {
                val values = binding.sliderTempRange.values
                if (values.size >= 2) {
                    val min = values[0]
                    val max = values[1]
                    // Cập nhật label text
                    binding.tvTempMinValue.text = "${min.toInt()}°C"
                    binding.tvTempMaxValue.text = "${max.toInt()}°C"
                    // Màu text đỏ nếu min >= max
                    val isValid = min < max
                    binding.tvTempMinValue.setTextColor(
                        if (isValid) Color.parseColor("#64B5F6") else Color.parseColor("#EF5350"))
                    binding.tvTempWarning.visibility = if (isValid) View.GONE else View.VISIBLE
                    // Cập nhật range preview bar
                    updateRangeBar(
                        track      = binding.viewTempRangeTrack,
                        bar        = binding.viewTempRangeBar,
                        minVal     = min,
                        maxVal     = max,
                        totalRange = 50f  // 0°C đến 50°C
                    )
                }
            }
        }

        // Listener cho slider độ ẩm
        binding.sliderHumidRange.addOnChangeListener { _, _, _ ->
            if (!isInitializingSliders) {
                val values = binding.sliderHumidRange.values
                if (values.size >= 2) {
                    val min = values[0]
                    val max = values[1]
                    binding.tvHumidMinValue.text = "${min.toInt()}%"
                    binding.tvHumidMaxValue.text = "${max.toInt()}%"
                    val isValid = min < max
                    binding.tvHumidMinValue.setTextColor(
                        if (isValid) Color.parseColor("#64B5F6") else Color.parseColor("#EF5350"))
                    binding.tvHumidWarning.visibility = if (isValid) View.GONE else View.VISIBLE
                    updateRangeBar(
                        track      = binding.viewHumidRangeTrack,
                        bar        = binding.viewHumidRangeBar,
                        minVal     = min,
                        maxVal     = max,
                        totalRange = 100f  // 0% đến 100%
                    )
                }
            }
        }
    }

    /**
     * updateRangeBar — Cập nhật width và margin của "Vùng an toàn" bar.
     *
     * Công thức:
     * safeStartRatio = minVal / totalRange          → marginStart
     * safeWidthRatio = (maxVal - minVal) / totalRange → width
     *
     * Cần đợi track được render xong để lấy width thật (dùng post{}).
     */
    private fun updateRangeBar(
        track: View, bar: View,
        minVal: Float, maxVal: Float, totalRange: Float
    ) {
        track.post {
            val trackWidth = track.width
            if (trackWidth <= 0) return@post

            val safeStart = ((minVal / totalRange) * trackWidth).toInt()
            val safeWidth = (((maxVal - minVal) / totalRange) * trackWidth)
                .toInt().coerceAtLeast(4)  // tối thiểu 4dp để vẫn nhìn thấy

            val params = bar.layoutParams as? LinearLayout.LayoutParams ?: return@post
            params.marginStart = safeStart
            params.width = safeWidth
            bar.layoutParams = params
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE — Nhận threshold và plant từ SharedDeviceViewModel
    // ─────────────────────────────────────────────────────────────────────
    private fun observeSharedViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // ── Threshold hiện tại → khởi tạo slider ─────────────
                // Chỉ set 1 lần khi lần đầu vào màn hình (isInitializingSliders)
                launch {
                    sharedViewModel.thresholds.collectLatest { t ->
                        isInitializingSliders = true
                        try {
                            // RangeSlider nhận List<Float> = [min, max]
                            binding.sliderTempRange.values = listOf(
                                t.tempMin.toFloat().coerceIn(0f, 49f),
                                t.tempMax.toFloat().coerceIn(1f, 50f)
                            )
                            binding.sliderHumidRange.values = listOf(
                                t.humidMin.toFloat().coerceIn(0f, 99f),
                                t.humidMax.toFloat().coerceIn(1f, 100f)
                            )
                            // Cập nhật labels
                            binding.tvTempMinValue.text  = "${t.tempMin.toInt()}°C"
                            binding.tvTempMaxValue.text  = "${t.tempMax.toInt()}°C"
                            binding.tvHumidMinValue.text = "${t.humidMin.toInt()}%"
                            binding.tvHumidMaxValue.text = "${t.humidMax.toInt()}%"
                        } finally {
                            isInitializingSliders = false
                        }

                        // Cập nhật range bar sau khi slider đã set xong
                        binding.viewTempRangeTrack.post {
                            updateRangeBar(binding.viewTempRangeTrack, binding.viewTempRangeBar,
                                t.tempMin.toFloat(), t.tempMax.toFloat(), 50f)
                            updateRangeBar(binding.viewHumidRangeTrack, binding.viewHumidRangeBar,
                                t.humidMin.toFloat(), t.humidMax.toFloat(), 100f)
                        }
                    }
                }

                // ── Plant đang chọn → hiện tên + lưu default ─────────
                launch {
                    sharedViewModel.selectedPlant.collectLatest { plant ->
                        plant ?: return@collectLatest
                        binding.tvCurrentPlant.text = "${plant.icon} ${plant.name}"
                        // Lưu default để nút "Đặt lại" dùng
                        plantTempMin  = plant.tempMin.toFloat()
                        plantTempMax  = plant.tempMax.toFloat()
                        plantHumidMin = plant.humidityMin.toFloat()
                        plantHumidMax = plant.humidityMax.toFloat()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CLICK LISTENERS
    // ─────────────────────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // ── Nút Lưu ngưỡng ────────────────────────────────────────────
        binding.btnSave.setOnClickListener {
            val tempValues  = binding.sliderTempRange.values
            val humidValues = binding.sliderHumidRange.values

            if (tempValues.size < 2 || humidValues.size < 2) return@setOnClickListener

            val tempMin  = tempValues[0].toDouble()
            val tempMax  = tempValues[1].toDouble()
            val humidMin = humidValues[0].toDouble()
            val humidMax = humidValues[1].toDouble()

            // Validate
            if (tempMin >= tempMax) {
                Snackbar.make(binding.root, "⚠️ Nhiệt độ min phải nhỏ hơn max", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(Color.parseColor("#8B1A1A")).show()
                return@setOnClickListener
            }
            if (humidMin >= humidMax) {
                Snackbar.make(binding.root, "⚠️ Độ ẩm min phải nhỏ hơn max", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(Color.parseColor("#8B1A1A")).show()
                return@setOnClickListener
            }

            // Disable button + loading state
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Đang lưu..."

            // ── Gọi SharedDeviceViewModel.saveThresholds() ────────────
            // Hàm này ghi vào Firebase: GreenHouseSystem/users/$uid/customThresholds
            // AlertWorker sẽ đọc path này khi chạy tiếp theo
            sharedViewModel.saveThresholds(
                tempMin, tempMax, humidMin, humidMax,
                onSuccess = {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "💾  Lưu ngưỡng"
                    // Bounce animation khi thành công
                    binding.btnSave.animate()
                        .scaleX(1.06f).scaleY(1.06f).setDuration(120L)
                        .withEndAction {
                            binding.btnSave.animate()
                                .scaleX(1f).scaleY(1f).setDuration(300L)
                                .setInterpolator(OvershootInterpolator(4f)).start()
                        }.start()
                    Snackbar.make(binding.root, "✅ Đã lưu ngưỡng cảnh báo!", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.parseColor("#1B4332"))
                        .setTextColor(Color.parseColor("#CCFF00"))
                        .show()
                },
                onError = { msg ->
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "💾  Lưu ngưỡng"
                    Snackbar.make(binding.root, "Lỗi: $msg", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#8B1A1A"))
                        .setTextColor(Color.parseColor("#FFCDD2"))
                        .show()
                }
            )
        }

        // ── Nút Đặt lại → reset về giá trị plant default ─────────────
        binding.btnReset.setOnClickListener {
            isInitializingSliders = true
            try {
                binding.sliderTempRange.values = listOf(
                    plantTempMin.coerceIn(0f, 49f),
                    plantTempMax.coerceIn(1f, 50f)
                )
                binding.sliderHumidRange.values = listOf(
                    plantHumidMin.coerceIn(0f, 99f),
                    plantHumidMax.coerceIn(1f, 100f)
                )
                binding.tvTempMinValue.text  = "${plantTempMin.toInt()}°C"
                binding.tvTempMaxValue.text  = "${plantTempMax.toInt()}°C"
                binding.tvHumidMinValue.text = "${plantHumidMin.toInt()}%"
                binding.tvHumidMaxValue.text = "${plantHumidMax.toInt()}%"
                binding.tvTempWarning.visibility  = View.GONE
                binding.tvHumidWarning.visibility = View.GONE
            } finally {
                isInitializingSliders = false
            }
            // Update range bars
            binding.viewTempRangeTrack.post {
                updateRangeBar(binding.viewTempRangeTrack, binding.viewTempRangeBar,
                    plantTempMin, plantTempMax, 50f)
                updateRangeBar(binding.viewHumidRangeTrack, binding.viewHumidRangeBar,
                    plantHumidMin, plantHumidMax, 100f)
            }
            Snackbar.make(binding.root, "Đã đặt lại về mặc định", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#1A2D1E")).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
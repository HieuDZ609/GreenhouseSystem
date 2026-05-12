package com.example.greenhousesystem.ui.control

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentControlBinding
import com.example.greenhousesystem.model.LedMode
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ControlFragment — Điều khiển LED RGB.
 *
 * ✅ FIX 1: Xóa hoàn toàn dòng:
 *   sharedViewModel.selectedDevice.observe(...)
 *   → selectedDevice không tồn tại trong SharedDeviceViewModel.
 *   SharedDeviceViewModel dùng StateFlow, không phải LiveData.
 *
 * ✅ FIX 2: Thay observe() bằng collectLatest() với repeatOnLifecycle
 *   để tương thích với StateFlow từ SharedDeviceViewModel.
 *
 * ✅ FIX 3: Dùng sharedViewModel.ledStatus (StateFlow) thay vì
 *   viewModel.config (LiveData từ ControlViewModel riêng),
 *   vì SharedDeviceViewModel là nguồn dữ liệu duy nhất (single source).
 *
 * ✅ FIX 4: Gọi sharedViewModel.applyLedMode() / applyManualColor() /
 *   toggleLed() thay vì viewModel.* để data flow nhất quán.
 */
class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    // ✅ FIX: Chỉ dùng SharedDeviceViewModel (activityViewModels)
    //         Không cần ControlViewModel riêng nữa vì tất cả logic
    //         LED đã nằm trong SharedDeviceViewModel.
    private val sharedViewModel: SharedDeviceViewModel by activityViewModels()

    // Tránh vòng lặp khi set UI từ code (không phải từ user)
    private var isUpdatingFromCode = false

    // Mode người dùng đang chọn (null = chưa chọn, dùng giá trị từ Firebase)
    private var currentSelectedMode: LedMode? = null

    // Trạng thái LED hiện tại (đọc từ StateFlow)
    private var isLedOn = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ FIX: KHÔNG gọi sharedViewModel.selectedDevice ở đây
        //         vì field đó không tồn tại.
        //         SharedDeviceViewModel tự load data trong init{}.

        setupPowerButton()
        setupPresetCards()
        setupSliders()
        setupApplyButton()
        observeViewModel()   // Dùng StateFlow thay vì LiveData
    }

    // ─────────────────────────────────────────────────────────────────
    //  POWER BUTTON — Bật / Tắt LED
    // ─────────────────────────────────────────────────────────────────
    private fun setupPowerButton() {
        binding.btnPower.setOnClickListener {
            // 1. Tính toán trạng thái ngược lại (Đang sáng -> Tắt)
            val currentOnState = sharedViewModel.ledStatus.value.isOn
            val newState = !currentOnState

            // 2. Gửi lệnh lên Firebase
            sharedViewModel.toggleLed(newState)

            // 3. Cập nhật lại màu của nút ngay lập tức
            updatePowerUi(newState)

        }
    }

    /**
     * updatePowerUi — Mờ/sáng các preset card theo trạng thái LED.
     * Alpha 0.4 khi tắt để thể hiện rằng các mode không active.
     */
    private fun updatePowerUi(isOn: Boolean) {
        isLedOn = isOn
        val alpha = if (isOn) 1f else 0.4f

        listOf(
            binding.cardGermination,
            binding.cardVegetative,
            binding.cardFlowering,
            binding.cardFruiting,
            binding.cardManual
        ).forEach { card ->
            card.alpha     = alpha
            card.isEnabled = isOn
        }

        binding.cardSlider.alpha = alpha

        // Glow nút power: sáng lime khi bật, mờ khi tắt
        binding.viewPowerGlow.alpha = if (isOn) 1f else 0.3f

        // Màu icon power
        binding.iconPower.apply {
            val tintColor = if (isOn) {
                resources.getColor(R.color.lime_accent, null)   // #CCFF00
            } else {
                resources.getColor(R.color.color_offline, null) // #4A5A4E
            }
            binding.iconPower.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRESET CARDS — Chọn chế độ chiếu sáng
    // ─────────────────────────────────────────────────────────────────
    private fun setupPresetCards() {
        binding.cardGermination.setOnClickListener { selectMode(LedMode.GERMINATION) }
        binding.cardVegetative.setOnClickListener  { selectMode(LedMode.VEGETATIVE)  }
        binding.cardFlowering.setOnClickListener   { selectMode(LedMode.FLOWERING)   }
        binding.cardFruiting.setOnClickListener    { selectMode(LedMode.FRUITING)    }
        binding.cardManual.setOnClickListener      { selectMode(LedMode.MANUAL)      }
    }

    private fun selectMode(mode: LedMode) {
        currentSelectedMode = mode
        highlightSelectedCard(mode)

        if (mode == LedMode.MANUAL) {
            // Hiện Color Orbit wheel + sliders
            showManualCard(true)
        } else {
            // Ẩn sliders và apply preset ngay lập tức
            showManualCard(false)
            // ✅ Gọi SharedDeviceViewModel.applyLedMode()
            sharedViewModel.applyLedMode(mode)
            showSuccessAnimation()
        }
    }

    /**
     * highlightSelectedCard — Stroke lime cho card đang chọn,
     * reset các card còn lại về stroke mờ.
     */
    private fun highlightSelectedCard(mode: LedMode) {
        val selectedStrokeColor = requireContext().getColor(R.color.lime_accent)  // #CCFF00
        val defaultStrokeColor  = requireContext().getColor(R.color.card_stroke_dark)

        val allCards = listOf(
            binding.cardGermination to LedMode.GERMINATION,
            binding.cardVegetative  to LedMode.VEGETATIVE,
            binding.cardFlowering   to LedMode.FLOWERING,
            binding.cardFruiting    to LedMode.FRUITING,
            binding.cardManual      to LedMode.MANUAL
        )

        allCards.forEach { (card, cardMode) ->
            val isSelected = cardMode == mode
            card.strokeColor  = if (isSelected) selectedStrokeColor else defaultStrokeColor
            card.strokeWidth  = if (isSelected) 4 else 1

            // Spring scale: card được chọn phóng to nhẹ
            card.animate()
                .scaleX(if (isSelected) 1.04f else 1f)
                .scaleY(if (isSelected) 1.04f else 1f)
                .setDuration(200L)
                .setInterpolator(OvershootInterpolator(3f))
                .start()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  MANUAL CARD — Hiện / Ẩn với animation
    // ─────────────────────────────────────────────────────────────────
    private fun showManualCard(show: Boolean) {
        binding.cardSlider.apply {
            if (show) {
                visibility = View.VISIBLE
                alpha  = 0f; scaleX = 0.9f; scaleY = 0.9f
                animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(350L)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            } else {
                animate().alpha(0f).scaleX(0.9f).scaleY(0.9f)
                    .setDuration(200L)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  SLIDERS — R / G / B manual color
    // ─────────────────────────────────────────────────────────────────
    private fun setupSliders() {
        binding.sliderRed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvRedValue.text = value.toInt().toString()
                updateManualPreview()
            }
        }
        binding.sliderGreen.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvGreenValue.text = value.toInt().toString()
                updateManualPreview()
            }
        }
        binding.sliderBlue.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvBlueValue.text = value.toInt().toString()
                updateManualPreview()
            }
        }

        // Color Orbit wheel sync ngược lại slider
        binding.colorOrbitWheel.onColorChanged = { r, g, b ->
            isUpdatingFromCode = true
            binding.sliderRed.value   = r.toFloat()
            binding.sliderGreen.value = g.toFloat()
            binding.sliderBlue.value  = b.toFloat()
            binding.tvRedValue.text   = r.toString()
            binding.tvGreenValue.text = g.toString()
            binding.tvBlueValue.text  = b.toString()
            isUpdatingFromCode = false
            updateManualPreview()
        }
    }

    private fun updateManualPreview() {
        val r = binding.sliderRed.value.toInt()
        val g = binding.sliderGreen.value.toInt()
        val b = binding.sliderBlue.value.toInt()
        val color = Color.rgb(r, g, b)
        binding.viewManualPreview.setCardBackgroundColor(color)
        // Cập nhật orb preview trong header
        binding.viewLedPreview.setCardBackgroundColor(color)
    }

    private fun setupApplyButton() {
        binding.btnApplyManual.setOnClickListener {
            val r = binding.sliderRed.value.toInt()
            val g = binding.sliderGreen.value.toInt()
            val b = binding.sliderBlue.value.toInt()
            // ✅ Gọi SharedDeviceViewModel.applyManualColor()
            sharedViewModel.applyManualColor(r, g, b)
            showSuccessAnimation()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  OBSERVE SharedDeviceViewModel với StateFlow
    //
    //  ✅ FIX: Thay viewModel.config.observe() (LiveData) bằng
    //          collectLatest() từ sharedViewModel.ledStatus (StateFlow).
    //
    //  repeatOnLifecycle(STARTED): tự pause khi Fragment bị ẩn,
    //  resume khi quay lại → tránh memory leak và update thừa.
    // ─────────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // ── LED status từ Firebase ────────────────────────────
                launch {
                    sharedViewModel.ledStatus.collectLatest { led ->
                        isUpdatingFromCode = true

                        // Trạng thái bật/tắt
                        updatePowerUi(led.isOn)

                        // Màu preview orb
                        val color = Color.rgb(led.red, led.green, led.blue)
                        binding.viewLedPreview.setCardBackgroundColor(color)

                        // Mode text + RGB text
                        val mode = try {
                            LedMode.valueOf(led.mode)
                        } catch (e: Exception) {
                            LedMode.MANUAL
                        }
                        binding.tvCurrentMode.text = "${mode.icon} ${mode.displayName}"
                        binding.tvColorHex.text    =
                            "R:${led.red}  G:${led.green}  B:${led.blue}"

                        // Chỉ sync UI mode nếu user chưa chọn gì
                        if (currentSelectedMode == null) {
                            highlightSelectedCard(mode)
                            if (mode == LedMode.MANUAL) {
                                showManualCard(true)
                                // Sync slider về giá trị Firebase
                                binding.sliderRed.value   = led.red.toFloat()
                                binding.sliderGreen.value = led.green.toFloat()
                                binding.sliderBlue.value  = led.blue.toFloat()
                                binding.tvRedValue.text   = led.red.toString()
                                binding.tvGreenValue.text = led.green.toString()
                                binding.tvBlueValue.text  = led.blue.toString()
                                // Sync Color Orbit wheel
                                binding.colorOrbitWheel.setColor(led.red, led.green, led.blue)
                                updateManualPreview()
                            }
                        }

                        isUpdatingFromCode = false
                    }
                }

                // ── Loading indicator ─────────────────────────────────
                // SharedDeviceViewModel không có isLoading riêng cho LED,
                // có thể thêm nếu cần. Hiện tại ẩn progressLed.
                binding.progressLed.visibility = View.GONE
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  SUCCESS ANIMATION — Popup Lottie check khi apply thành công
    // ─────────────────────────────────────────────────────────────────
    private fun showSuccessAnimation() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.layout_lottie_popup, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val lottieView = dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(
            R.id.lottieAnimation
        )
        lottieView.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationEnd(animation: android.animation.Animator) { dialog.dismiss() }
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
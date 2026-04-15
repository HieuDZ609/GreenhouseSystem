package com.example.greenhousesystem.ui.control

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentControlBinding
import com.example.greenhousesystem.model.LedMode
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ControlViewModel by viewModels()

    private var isUpdatingSwitch = false
    private var currentSelectedMode: LedMode? = null

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
        setupLedSwitch()
        setupPresetCards()
        setupSliders()
        setupApplyButton()
        observeViewModel()
    }

    private fun setupLedSwitch() {
        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                viewModel.toggleLed(isChecked)
                // Dim toàn bộ preset khi tắt
                val alpha = if (isChecked) 1f else 0.4f
                binding.cardPreset.alpha = alpha
                binding.cardSlider.alpha = alpha
                binding.cardPreset.isEnabled = isChecked
            }
        }
    }

    private fun setupPresetCards() {
        binding.cardGermination.setOnClickListener {
            selectMode(LedMode.GERMINATION)
        }
        binding.cardVegetative.setOnClickListener {
            selectMode(LedMode.VEGETATIVE)
        }
        binding.cardFlowering.setOnClickListener {
            selectMode(LedMode.FLOWERING)
        }
        binding.cardFruiting.setOnClickListener {
            selectMode(LedMode.FRUITING)
        }
        binding.cardManual.setOnClickListener {
            selectMode(LedMode.MANUAL)
        }
    }

    private fun selectMode(mode: LedMode) {
        currentSelectedMode = mode
        highlightSelectedCard(mode)

        if (mode == LedMode.MANUAL) {
            // Hiện slider
            binding.cardSlider.visibility = View.VISIBLE
        } else {
            // Ẩn slider và áp dụng preset luôn
            binding.cardSlider.visibility = View.GONE
            viewModel.applyPresetMode(mode)
        }
    }

    private fun highlightSelectedCard(mode: LedMode) {
        val selectedColor = requireContext().getColor(R.color.green_primary)
        val defaultColor = requireContext().getColor(R.color.gray)

        // Reset tất cả về màu mặc định
        listOf(
            binding.cardGermination,
            binding.cardVegetative,
            binding.cardFlowering,
            binding.cardFruiting,
            binding.cardManual
        ).forEach { card ->
            card.strokeColor = defaultColor
            card.strokeWidth = 2
        }

        // Highlight card được chọn
        val selectedCard: MaterialCardView = when (mode) {
            LedMode.GERMINATION -> binding.cardGermination
            LedMode.VEGETATIVE -> binding.cardVegetative
            LedMode.FLOWERING -> binding.cardFlowering
            LedMode.FRUITING -> binding.cardFruiting
            LedMode.MANUAL -> binding.cardManual
        }
        selectedCard.strokeColor = selectedColor
        selectedCard.strokeWidth = 8
    }

    private fun setupSliders() {
        val sliderListener = Slider.OnChangeListener { slider, value, fromUser ->
            if (fromUser) updateManualPreview()
        }

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
    }

    private fun updateManualPreview() {
        val r = binding.sliderRed.value.toInt()
        val g = binding.sliderGreen.value.toInt()
        val b = binding.sliderBlue.value.toInt()
        val color = Color.rgb(r, g, b)
        binding.viewManualPreview.setBackgroundColor(color)
    }

    private fun setupApplyButton() {
        binding.btnApplyManual.setOnClickListener {
            val r = binding.sliderRed.value.toInt()
            val g = binding.sliderGreen.value.toInt()
            val b = binding.sliderBlue.value.toInt()
            viewModel.applyManualColor(r, g, b)
        }
    }

    private fun observeViewModel() {
        viewModel.ledStatus.observe(viewLifecycleOwner) { led ->
            // Cập nhật switch
            isUpdatingSwitch = true
            binding.switchLed.isChecked = led.isOn
            isUpdatingSwitch = false

            // Dim khi tắt
            val alpha = if (led.isOn) 1f else 0.4f
            binding.cardPreset.alpha = alpha
            binding.cardSlider.alpha = alpha

            // Preview màu tổng quan
            val color = Color.rgb(led.red, led.green, led.blue)
            binding.viewLedPreview.setBackgroundColor(color)

            // Mode hiện tại
            val mode = try {
                LedMode.valueOf(led.mode)
            } catch (e: Exception) { LedMode.MANUAL }

            binding.tvCurrentMode.text = "${mode.icon} ${mode.displayName}"
            binding.tvColorHex.text = "R:${led.red}  G:${led.green}  B:${led.blue}"

            // Highlight card đang active (từ Firebase)
            if (currentSelectedMode == null) {
                highlightSelectedCard(mode)
                if (mode == LedMode.MANUAL) {
                    binding.cardSlider.visibility = View.VISIBLE
                    // Sync slider với giá trị từ Firebase
                    binding.sliderRed.value = led.red.toFloat()
                    binding.sliderGreen.value = led.green.toFloat()
                    binding.sliderBlue.value = led.blue.toFloat()
                    binding.tvRedValue.text = led.red.toString()
                    binding.tvGreenValue.text = led.green.toString()
                    binding.tvBlueValue.text = led.blue.toString()
                    updateManualPreview()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressLed.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnApplyManual.isEnabled = !loading
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearActionResult()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
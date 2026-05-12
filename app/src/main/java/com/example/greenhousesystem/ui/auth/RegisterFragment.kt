package com.example.greenhousesystem.ui.auth

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentRegisterBinding
import com.example.greenhousesystem.utils.ValidationUtils
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEntrance()
        setupRealTimeValidation()
        setupClickListeners()
        observeViewModel()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ENTRANCE ANIMATION
    // ─────────────────────────────────────────────────────────────────────
    private fun setupEntrance() {
        binding.root.apply {
            alpha        = 0f
            translationY = 40f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600L)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  REAL-TIME VALIDATION
    //  Lưu ý: Chỉ dùng các ID có trong fragment_register.xml hiện tại:
    //  etFullName, etEmail, etPhone, etPassword, etConfirmPassword
    //  tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword
    //  progressPasswordStrength, tvPasswordStrength (đã thêm vào XML)
    // ─────────────────────────────────────────────────────────────────────
    private fun setupRealTimeValidation() {

        // Họ tên — validate khi rời focus
        binding.etFullName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = binding.etFullName.text.toString().trim()
                ValidationUtils.validateDisplayName(name)
                    ?.let { binding.tilFullName.error = it }
                    ?: binding.tilFullName.clearError()
            }
        }

        // Email — validate real-time
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s?.toString()?.trim() ?: return
                if (email.length > 5) {
                    ValidationUtils.validateEmail(email)
                        ?.let { binding.tilEmail.error = it }
                        ?: binding.tilEmail.clearError()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Mật khẩu — strength bar + cross-check
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pass = s?.toString() ?: return
                if (pass.isNotEmpty()) {
                    binding.progressPasswordStrength.visibility = View.VISIBLE
                }
                updatePasswordStrength(pass)

                val confirm = binding.etConfirmPassword.text?.toString() ?: ""
                if (confirm.isNotEmpty()) {
                    ValidationUtils.validateConfirmPassword(pass, confirm)
                        ?.let { binding.tilConfirmPassword.error = it }
                        ?: binding.tilConfirmPassword.clearError()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Xác nhận mật khẩu
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pass    = binding.etPassword.text?.toString() ?: ""
                val confirm = s?.toString() ?: return
                if (confirm.isNotEmpty()) {
                    ValidationUtils.validateConfirmPassword(pass, confirm)
                        ?.let { binding.tilConfirmPassword.error = it }
                        ?: binding.tilConfirmPassword.clearError()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PASSWORD STRENGTH
    // ─────────────────────────────────────────────────────────────────────
    private fun updatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            binding.progressPasswordStrength.visibility = View.INVISIBLE
            binding.tvPasswordStrength.text = ""
            return
        }

        var score = 0
        if (password.length >= 8)                         score++
        if (password.any { it.isUpperCase() })            score++
        if (password.any { it.isDigit() })                score++
        if (password.any { "!@#\$%^&*()".contains(it) }) score++

        val (colorHex, label) = when (score) {
            0, 1 -> "#EF5350" to "Yếu"
            2    -> "#FFB74D" to "Trung bình"
            3    -> "#84CC16" to "Khá mạnh"
            else -> "#CCFF00" to "Mạnh"
        }
        val parsedColor    = Color.parseColor(colorHex)
        val progressTarget = (score * 25).coerceIn(0, 100)

        binding.progressPasswordStrength.setIndicatorColor(parsedColor)
        ObjectAnimator.ofInt(
            binding.progressPasswordStrength,
            "progress",
            binding.progressPasswordStrength.progress,
            progressTarget
        ).apply { duration = 300L; start() }

        binding.tvPasswordStrength.text = label
        binding.tvPasswordStrength.setTextColor(parsedColor)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CLICK LISTENERS
    // ─────────────────────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnRegisterSubmit.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100L).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(300L)
                        .setInterpolator(OvershootInterpolator(4f))
                        .start()
                    validateAndRegister()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(200L).start()
                }
            }
            true
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun validateAndRegister() {
        val name    = binding.etFullName.text.toString().trim()
        val email   = binding.etEmail.text.toString().trim()
        val phone   = binding.etPhone.text.toString().trim()
        val pass    = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        listOf(
            binding.tilFullName, binding.tilEmail, binding.tilPhone,
            binding.tilPassword, binding.tilConfirmPassword
        ).forEach { it.error = null; it.isErrorEnabled = false }

        var isValid = true
        ValidationUtils.validateDisplayName(name)
            ?.let { binding.tilFullName.error = it; isValid = false }
        ValidationUtils.validateEmail(email)
            ?.let { binding.tilEmail.error = it; isValid = false }
        ValidationUtils.validatePhone(phone)
            ?.let { binding.tilPhone.error = it; isValid = false }
        ValidationUtils.validatePassword(pass)
            ?.let { binding.tilPassword.error = it; isValid = false }
        ValidationUtils.validateConfirmPassword(pass, confirm)
            ?.let { binding.tilConfirmPassword.error = it; isValid = false }

        if (isValid) viewModel.register(name, email, phone, pass)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE
    // ─────────────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnRegisterSubmit.isEnabled = false
                    binding.btnRegisterSubmit.text      = ""
                    binding.lottieLoading.visibility    = View.VISIBLE
                }
                is AuthState.Success -> {
                    binding.lottieLoading.visibility = View.GONE
                    showSuccessAndNavigate()
                }
                is AuthState.Error -> {
                    binding.btnRegisterSubmit.isEnabled = true
                    // ✅ FIX line 283: bỏ getString(R.string.register) vì string này
                    //    không tồn tại trong strings.xml — dùng string literal thay thế
                    binding.btnRegisterSubmit.text   = "Đăng ký"
                    binding.lottieLoading.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#8B1A1A"))
                        .setTextColor(Color.parseColor("#FFCDD2"))
                        .show()
                }
            }
        }
    }

    private fun showSuccessAndNavigate() {
        val dialogView = layoutInflater.inflate(R.layout.layout_lottie_popup, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogView.findViewById<LottieAnimationView>(R.id.lottieAnimation)
            .addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    dialog.dismiss()
                    findNavController().navigate(R.id.action_register_to_login)
                }
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

private fun TextInputLayout.clearError() {
    error          = null
    isErrorEnabled = false
}
package com.example.greenhousesystem.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentLoginBinding
import com.example.greenhousesystem.utils.ValidationUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAlreadyLoggedIn()
        observeViewModel()
        setupClickListeners()
    }

    private fun checkAlreadyLoggedIn() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            findNavController().navigate(R.id.action_login_to_dashboard)
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { validateAndLogin() }
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                Snackbar.make(binding.root, "Đã gửi email đặt lại mật khẩu!", Snackbar.LENGTH_LONG).show()
            } else {
                binding.tilEmail.error = "Nhập email để đặt lại mật khẩu"
            }
        }
    }

    private fun validateAndLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var isValid = true
        ValidationUtils.validateEmail(email)?.let {
            binding.tilEmail.error = it; isValid = false
        }
        if (password.isBlank()) {
            binding.tilPassword.error = "Vui lòng nhập mật khẩu"; isValid = false
        }

        if (isValid) viewModel.login(email, password)
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.lottieLoading.visibility = View.VISIBLE
                }
                is AuthState.Success -> {
                    binding.lottieLoading.visibility = View.GONE
                    findNavController().navigate(R.id.action_login_to_dashboard)
                }
                is AuthState.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.lottieLoading.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(R.color.red, null))
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
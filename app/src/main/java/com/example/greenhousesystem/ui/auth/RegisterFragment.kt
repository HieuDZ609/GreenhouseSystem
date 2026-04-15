package com.example.greenhousesystem.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.FragmentRegisterBinding
import com.example.greenhousesystem.utils.ValidationUtils
import com.google.android.material.snackbar.Snackbar


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener { validateAndRegister() }
        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun validateAndRegister() {
        val name = binding.etDisplayName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        // Clear lỗi cũ
        binding.tilDisplayName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        // Validate
        var isValid = true
        ValidationUtils.validateDisplayName(name)?.let {
            binding.tilDisplayName.error = it; isValid = false
        }
        ValidationUtils.validateEmail(email)?.let {
            binding.tilEmail.error = it; isValid = false
        }
        ValidationUtils.validatePhone(phone)?.let {
            binding.tilPhone.error = it; isValid = false
        }
        ValidationUtils.validatePassword(password)?.let {
            binding.tilPassword.error = it; isValid = false
        }
        ValidationUtils.validateConfirmPassword(password, confirm)?.let {
            binding.tilConfirmPassword.error = it; isValid = false
        }

        if (isValid) viewModel.register(name, email, phone, password)
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.lottieLoading.visibility = View.VISIBLE
                }
                is AuthState.Success -> {
                    binding.lottieLoading.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_register_to_login)
                }
                is AuthState.Error -> {
                    binding.btnRegister.isEnabled = true
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
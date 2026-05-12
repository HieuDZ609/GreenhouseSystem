package com.example.greenhousesystem.ui.drawer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.greenhousesystem.databinding.FragmentAccountBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
        .child("GreenHouseSystem")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserInfo()
        setupClickListeners()
    }

    private fun loadUserInfo() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        binding.tvName.text = user.displayName ?: "Chưa cập nhật"
        binding.tvEmail.text = user.email ?: ""
        binding.etDisplayName.setText(user.displayName)

        // Load phone từ Firebase
        db.child("users").child(uid).get().addOnSuccessListener { snap ->
            val phone = snap.child("phone").getValue(String::class.java) ?: ""
            binding.tvPhone.text = if (phone.isNotEmpty()) phone else "Chưa cập nhật"
            binding.etPhone.setText(phone)
        }
    }

    private fun setupClickListeners() {
        // Cập nhật tên
        binding.btnUpdateName.setOnClickListener {
            val newName = binding.etDisplayName.text.toString().trim()
            if (newName.isEmpty()) {
                binding.tilDisplayName.error = "Vui lòng nhập tên"
                return@setOnClickListener
            }
            updateDisplayName(newName)
        }

        // Cập nhật số điện thoại
        binding.btnUpdatePhone.setOnClickListener {
            val newPhone = binding.etPhone.text.toString().trim()
            updatePhone(newPhone)
        }

        // Đổi mật khẩu
        binding.btnChangePassword.setOnClickListener {
            val current = binding.etCurrentPassword.text.toString()
            val newPass = binding.etNewPassword.text.toString()
            val confirm = binding.etConfirmNewPassword.text.toString()
            changePassword(current, newPass, confirm)
        }

        // Quay lại
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateDisplayName(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest
            .Builder()
            .setDisplayName(name)
            .build()

        auth.currentUser?.updateProfile(profileUpdates)
            ?.addOnSuccessListener {
                db.child("users").child(uid)
                    .child("displayName").setValue(name)
                binding.tvName.text = name
                Snackbar.make(binding.root, "Đã cập nhật tên", Snackbar.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener {
                Snackbar.make(binding.root, "Lỗi: ${it.message}", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun updatePhone(phone: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("phone").setValue(phone)
            .addOnSuccessListener {
                binding.tvPhone.text = phone
                Snackbar.make(binding.root, "Đã cập nhật số điện thoại", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun changePassword(current: String, new: String, confirm: String) {
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmNewPassword.error = null

        if (current.isEmpty()) { binding.tilCurrentPassword.error = "Nhập mật khẩu hiện tại"; return }
        if (new.length < 8) { binding.tilNewPassword.error = "Mật khẩu phải có ít nhất 8 ký tự"; return }
        if (new != confirm) { binding.tilConfirmNewPassword.error = "Mật khẩu không khớp"; return }

        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email ?: "", current)

        // Re-authenticate trước khi đổi mật khẩu
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(new)
                    .addOnSuccessListener {
                        binding.etCurrentPassword.text?.clear()
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmNewPassword.text?.clear()
                        Snackbar.make(binding.root, "Đổi mật khẩu thành công!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Snackbar.make(binding.root, "Lỗi: ${it.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.tilCurrentPassword.error = "Mật khẩu hiện tại không đúng"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
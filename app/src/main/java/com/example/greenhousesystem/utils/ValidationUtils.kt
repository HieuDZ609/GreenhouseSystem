package com.example.greenhousesystem.utils

object ValidationUtils {

    fun validateDisplayName(name: String): String? {
        if (name.isBlank()) return "Vui lòng nhập họ tên"
        if (name.length < 2) return "Họ tên phải có ít nhất 2 ký tự"
        return null
    }

    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Vui lòng nhập email"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Email không đúng định dạng"
        return null
    }

    fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return "Vui lòng nhập số điện thoại"
        val cleaned = phone.replace(" ", "").replace("-", "")
        val vnPattern = Regex("^(0[3|5|7|8|9])[0-9]{8}$")
        if (!vnPattern.matches(cleaned)) return "Số điện thoại Việt Nam không hợp lệ"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Vui lòng nhập mật khẩu"
        if (password.length < 8) return "Mật khẩu phải có ít nhất 8 ký tự"
        if (!password.any { it.isUpperCase() }) return "Mật khẩu phải có ít nhất 1 chữ hoa"
        if (!password.any { it.isLowerCase() }) return "Mật khẩu phải có ít nhất 1 chữ thường"
        if (!password.any { it.isDigit() }) return "Mật khẩu phải có ít nhất 1 chữ số"
        if (!password.any { !it.isLetterOrDigit() }) return "Mật khẩu phải có ít nhất 1 ký tự đặc biệt"
        return null
    }

    fun validateConfirmPassword(password: String, confirm: String): String? {
        if (confirm.isBlank()) return "Vui lòng xác nhận mật khẩu"
        if (password != confirm) return "Mật khẩu xác nhận không khớp"
        return null
    }
}
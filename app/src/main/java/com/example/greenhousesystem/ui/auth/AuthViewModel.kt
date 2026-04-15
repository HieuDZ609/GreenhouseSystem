package com.example.greenhousesystem.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhousesystem.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Register state
    private val _registerState = MutableLiveData<AuthState>()
    val registerState: LiveData<AuthState> = _registerState

    // Login state
    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState

    fun register(displayName: String, email: String, phone: String, password: String) {
        _registerState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                // 1. Tạo tài khoản Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Không thể tạo tài khoản")

                // 2. Cập nhật displayName trong Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                result.user?.updateProfile(profileUpdates)?.await()

                // 3. Lưu thông tin vào Realtime Database
                val user = User(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    phone = phone,
                    createdAt = System.currentTimeMillis()
                )
                database.child("GreenHouseSystem")
                    .child("users")
                    .child(uid)
                    .setValue(user)
                    .await()

                _registerState.value = AuthState.Success("Đăng ký thành công!")

            } catch (e: FirebaseAuthUserCollisionException) {
                _registerState.value = AuthState.Error("Email này đã được sử dụng")
            } catch (e: Exception) {
                _registerState.value = AuthState.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }

    fun login(email: String, password: String) {
        _loginState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Đăng nhập thất bại")

                // Kiểm tra tài khoản bị khóa
                val snapshot = database.child("GreenHouseSystem")
                    .child("users").child(uid).get().await()

                val isLocked = snapshot.child("isLocked").getValue(Boolean::class.java) ?: false
                if (isLocked) {
                    auth.signOut()
                    _loginState.value = AuthState.Error("Tài khoản đã bị khóa do đăng nhập sai quá nhiều lần")
                    return@launch
                }

                // Reset loginFailCount khi đăng nhập thành công
                database.child("GreenHouseSystem")
                    .child("users").child(uid)
                    .child("loginFailCount").setValue(0).await()

                _loginState.value = AuthState.Success("Đăng nhập thành công!")

            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _loginState.value = AuthState.Error("Email hoặc mật khẩu không đúng")
                handleLoginFail(email)
            } catch (e: Exception) {
                _loginState.value = AuthState.Error(e.message ?: "Đăng nhập thất bại")
            }
        }
    }

    private fun handleLoginFail(email: String) {
        viewModelScope.launch {
            try {
                // Tìm user theo email để tăng loginFailCount
                val snapshot = database.child("GreenHouseSystem")
                    .child("users")
                    .orderByChild("email")
                    .equalTo(email)
                    .get().await()

                snapshot.children.firstOrNull()?.let { userSnap ->
                    val uid = userSnap.key ?: return@let
                    val failCount = (userSnap.child("loginFailCount")
                        .getValue(Int::class.java) ?: 0) + 1

                    val updates = mutableMapOf<String, Any>(
                        "loginFailCount" to failCount
                    )
                    // Khóa tài khoản nếu sai >= 5 lần
                    if (failCount >= 5) {
                        updates["isLocked"] = true
                    }
                    database.child("GreenHouseSystem")
                        .child("users").child(uid)
                        .updateChildren(updates).await()
                }
            } catch (_: Exception) {}
        }
    }
}

// Sealed class trạng thái
sealed class AuthState {
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
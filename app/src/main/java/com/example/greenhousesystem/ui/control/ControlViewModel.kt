package com.example.greenhousesystem.ui.control

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhousesystem.model.DeviceConfig
import com.example.greenhousesystem.model.LedMode
import com.example.greenhousesystem.repository.DeviceRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ControlViewModel : ViewModel() {

    private val repository = DeviceRepository()

    private val _config = MutableLiveData<DeviceConfig>()
    val config: LiveData<DeviceConfig> = _config

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _actionResult = MutableLiveData<String?>()
    val actionResult: LiveData<String?> = _actionResult

    private var currentDeviceId = ""

    fun loadDevice(deviceId: String) {
        if (deviceId == currentDeviceId) return
        currentDeviceId = deviceId

        viewModelScope.launch {
            repository.getConfigFlow(deviceId).collectLatest { cfg ->
                _config.value = cfg
            }
        }
    }

    fun toggleLed(isOn: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateLedConfig(
                currentDeviceId,
                mapOf("led_on" to isOn)
            )
            _actionResult.value = if (result.isSuccess)
                if (isOn) "Đã bật đèn LED" else "Đã tắt đèn LED"
            else "Lỗi: ${result.exceptionOrNull()?.message}"
            _isLoading.value = false
        }
    }

    fun applyPresetMode(mode: LedMode) {
        viewModelScope.launch {
            _isLoading.value = true
            val updates = mapOf(
                "led_mode" to mode.name,
                "led_red" to mode.red,
                "led_green" to mode.green,
                "led_blue" to mode.blue,
                "led_on" to true
            )
            val result = repository.updateLedConfig(currentDeviceId, updates)
            _actionResult.value = if (result.isSuccess)
                "Đã áp dụng chế độ ${mode.displayName}"
            else "Lỗi: ${result.exceptionOrNull()?.message}"
            _isLoading.value = false
        }
    }

    fun applyManualColor(red: Int, green: Int, blue: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val updates = mapOf(
                "led_mode" to "MANUAL",
                "led_red" to red,
                "led_green" to green,
                "led_blue" to blue
            )
            val result = repository.updateLedConfig(currentDeviceId, updates)
            _actionResult.value = if (result.isSuccess)
                "Đã áp dụng màu thủ công"
            else "Lỗi: ${result.exceptionOrNull()?.message}"
            _isLoading.value = false
        }
    }

    fun clearActionResult() { _actionResult.value = null }
}
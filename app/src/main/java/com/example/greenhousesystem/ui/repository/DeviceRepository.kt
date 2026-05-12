package com.example.greenhousesystem.repository

import com.example.greenhousesystem.model.Device
import com.example.greenhousesystem.model.DeviceConfig
import com.example.greenhousesystem.model.Telemetry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DeviceRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    val uid get() = auth.currentUser?.uid ?: ""

    // ── Lấy danh sách thiết bị của user ──────────────────────
    fun getDevicesFlow(): Flow<List<Device>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = mutableListOf<Device>()
                snapshot.children.forEach { child ->
                    val ownerUid = child.child("owner_uid")
                        .getValue(String::class.java) ?: ""
                    if (ownerUid == uid) {
                        devices.add(
                            Device(
                                deviceId = child.key ?: "",
                                ownerUid = ownerUid,
                                deviceName = child.child("device_name")
                                    .getValue(String::class.java) ?: "",
                                status = child.child("status")
                                    .getValue(String::class.java) ?: "offline",
                                lastActive = child.child("last_active")
                                    .getValue(Long::class.java) ?: 0L
                            )
                        )
                    }
                }
                trySend(devices)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("Devices").addValueEventListener(listener)
        awaitClose { db.child("Devices").removeEventListener(listener) }
    }

    // ── Lắng nghe Telemetry realtime ─────────────────────────
    fun getTelemetryFlow(deviceId: String): Flow<Telemetry> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    Telemetry(
                        currentTemp = snapshot.child("current_temp")
                            .getValue(Double::class.java) ?: 0.0,
                        currentHumid = snapshot.child("current_humid")
                            .getValue(Double::class.java) ?: 0.0,
                        timestamp = snapshot.child("timestamp")
                            .getValue(Long::class.java) ?: 0L
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("Telemetry").child(deviceId)
            .addValueEventListener(listener)
        awaitClose {
            db.child("Telemetry").child(deviceId).removeEventListener(listener)
        }
    }

    // ── Lắng nghe Config realtime ─────────────────────────────
    fun getConfigFlow(deviceId: String): Flow<DeviceConfig> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    DeviceConfig(
                        ledOn = snapshot.child("led_on")
                            .getValue(Boolean::class.java) ?: false,
                        ledRed = snapshot.child("led_red")
                            .getValue(Int::class.java) ?: 0,
                        ledGreen = snapshot.child("led_green")
                            .getValue(Int::class.java) ?: 0,
                        ledBlue = snapshot.child("led_blue")
                            .getValue(Int::class.java) ?: 0,
                        ledMode = snapshot.child("led_mode")
                            .getValue(String::class.java) ?: "MANUAL",
                        tempMax = snapshot.child("temp_max")
                            .getValue(Double::class.java) ?: 35.0,
                        tempMin = snapshot.child("temp_min")
                            .getValue(Double::class.java) ?: 15.0,
                        humidMax = snapshot.child("humid_max")
                            .getValue(Double::class.java) ?: 90.0,
                        humidMin = snapshot.child("humid_min")
                            .getValue(Double::class.java) ?: 40.0,
                        selectedPlant = snapshot.child("selected_plant")
                            .getValue(String::class.java) ?: "TOMATO"
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("Configs").child(deviceId).addValueEventListener(listener)
        awaitClose {
            db.child("Configs").child(deviceId).removeEventListener(listener)
        }
    }

    // ── Đăng ký thiết bị mới (từ QR Code) ───────────────────
    suspend fun registerDevice(
        macAddress: String,
        deviceName: String
    ): Result<Unit> {
        return try {
            // Kiểm tra thiết bị đã có chủ chưa
            val existing = db.child("Devices").child(macAddress).get().await()
            if (existing.exists()) {
                val existingOwner = existing.child("owner_uid")
                    .getValue(String::class.java)
                if (existingOwner != null && existingOwner != uid) {
                    return Result.failure(Exception("Thiết bị này đã thuộc về người khác"))
                }
            }

            // Ghi thông tin thiết bị
            val deviceData = mapOf(
                "owner_uid" to uid,
                "device_name" to deviceName,
                "status" to "offline",
                "last_active" to System.currentTimeMillis()
            )
            db.child("Devices").child(macAddress)
                .setValue(deviceData).await()

            // Tạo config mặc định
            val defaultConfig = mapOf(
                "led_on" to false,
                "led_red" to 0,
                "led_green" to 0,
                "led_blue" to 0,
                "led_mode" to "MANUAL",
                "temp_max" to 35.0,
                "temp_min" to 15.0,
                "humid_max" to 90.0,
                "humid_min" to 40.0,
                "selected_plant" to "TOMATO"
            )
            db.child("Configs").child(macAddress)
                .setValue(defaultConfig).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Cập nhật LED Config ───────────────────────────────────
    suspend fun updateLedConfig(
        deviceId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            db.child("Configs").child(deviceId)
                .updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Cập nhật ngưỡng cảnh báo ─────────────────────────────
    suspend fun updateThresholds(
        deviceId: String,
        tempMin: Double,
        tempMax: Double,
        humidMin: Double,
        humidMax: Double
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "temp_min" to tempMin,
                "temp_max" to tempMax,
                "humid_min" to humidMin,
                "humid_max" to humidMax
            )
            db.child("Configs").child(deviceId)
                .updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Lưu FCM Token ─────────────────────────────────────────
    suspend fun saveFcmToken(token: String) {
        try {
            db.child("Users").child(uid)
                .child("fcmToken").setValue(token).await()
        } catch (e: Exception) { }
    }
}
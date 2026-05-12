package com.example.greenhousesystem.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class SensorHistoryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = FirebaseDatabase.getInstance().reference
        .child("GreenHouseSystem")

    override suspend fun doWork(): Result {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.success()

            // Lấy tất cả thiết bị của user
            val devicesSnap = db.child("Devices").get().await()

            devicesSnap.children.forEach { deviceChild ->
                val ownerUid = deviceChild.child("owner_uid")
                    .getValue(String::class.java) ?: return@forEach
                if (ownerUid != uid) return@forEach

                val deviceId = deviceChild.key ?: return@forEach

                // Lấy telemetry hiện tại
                val telemetry = db.child("Telemetry").child(deviceId).get().await()
                val temp = telemetry.child("current_temp")
                    .getValue(Double::class.java) ?: return@forEach
                val humid = telemetry.child("current_humid")
                    .getValue(Double::class.java) ?: return@forEach
                val timestamp = telemetry.child("timestamp")
                    .getValue(Long::class.java) ?: System.currentTimeMillis()

                // Lưu vào TelemetryHistory/{deviceId}
                val historyRef = db.child("TelemetryHistory")
                    .child(deviceId).push()
                historyRef.setValue(
                    mapOf(
                        "temp" to temp,
                        "humid" to humid,
                        "timestamp" to timestamp
                    )
                ).await()

                // Dọn dữ liệu cũ hơn 24 giờ
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val oldSnap = db.child("TelemetryHistory").child(deviceId)
                    .orderByChild("timestamp")
                    .endAt(oneDayAgo.toDouble())
                    .get().await()
                oldSnap.children.forEach { it.ref.removeValue().await() }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun cleanOldHistory() {
        try {
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val oldSnap = db.child("sensorHistory")
                .orderByChild("timestamp")
                .endAt(oneDayAgo.toDouble())
                .get().await()

            // Xóa các record cũ hơn 24 giờ
            oldSnap.children.forEach { child ->
                child.ref.removeValue().await()
            }
        } catch (e: Exception) {
            // Bỏ qua lỗi dọn dẹp
        }
    }
}
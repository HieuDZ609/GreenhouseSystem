package com.example.greenhousesystem.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.greenhousesystem.MainActivity
import com.example.greenhousesystem.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * AlertWorker — Background Worker kiểm tra ngưỡng và tạo cảnh báo.
 *
 * ══ VỊ TRÍ TRONG KIẾN TRÚC ══
 * AlertWorker là "nguồn gốc" tạo ra các entry trong Firebase
 * notifications/$uid. NotificationViewModel chỉ đọc dữ liệu đó.
 *
 * Luồng hoàn chỉnh:
 * 1. WorkManager kích hoạt AlertWorker mỗi 15 phút
 * 2. Worker đọc GreenHouseSystem/sensors (nhiệt độ, độ ẩm mới nhất)
 * 3. Worker đọc GreenHouseSystem/users/$uid/customThresholds
 *    (nếu không có → fallback về plantProfiles/$plantId)
 * 4. So sánh sensor vs threshold:
 *    - Nếu vượt ngưỡng → ghi entry vào notifications/$uid
 *    - Đồng thời → trigger local notification (NotificationCompat)
 * 5. Result.success() → Worker hoàn thành, WorkManager lên lịch lần sau
 *    Result.retry()   → Xảy ra lỗi, WorkManager thử lại sau 30 phút
 *
 * ══ CHỐNG SPAM THÔNG BÁO ══
 * Mỗi loại cảnh báo (TEMP_HIGH, TEMP_LOW, HUMID_HIGH, HUMID_LOW)
 * được throttle: chỉ gửi 1 thông báo mỗi 30 phút (cooldown check).
 * Cooldown được lưu trong SharedPreferences của Worker.
 *
 * ══ DEPENDENCY ══
 * Thêm vào build.gradle.kts:
 *   implementation("androidx.work:work-runtime-ktx:2.9.0")
 */
class AlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = FirebaseDatabase.getInstance().reference.child("GreenHouseSystem")

    // SharedPreferences để lưu thời điểm gửi cảnh báo cuối (chống spam)
    private val prefs = context.getSharedPreferences("alert_cooldown", Context.MODE_PRIVATE)

    // Cooldown 30 phút (ms) — không gửi cùng loại cảnh báo trong khoảng này
    private val cooldownMs = 30 * 60 * 1000L

    companion object {
        const val CHANNEL_ID   = "greenhouse_alerts"
        const val CHANNEL_NAME = "Cảnh báo nhà kính"

        /**
         * schedule — Đăng ký PeriodicWorkRequest với WorkManager.
         * Gọi từ: AuthViewModel.login() sau khi đăng nhập thành công.
         * Gọi từ: SplashActivity nếu user đã đăng nhập.
         *
         * ExistingPeriodicWorkPolicy.KEEP: Nếu đã có work → giữ nguyên,
         * không tạo duplicate. Đảm bảo chỉ chạy 1 worker tại 1 thời điểm.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Chỉ chạy khi có mạng
                .build()

            val request = PeriodicWorkRequestBuilder<AlertWorker>(
                15, TimeUnit.MINUTES        // Chạy mỗi 15 phút
            )
                .setConstraints(constraints)
                .setBackoffCriteria(         // Retry sau 10 phút nếu lỗi
                    BackoffPolicy.LINEAR,
                    10, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "greenhouse_alert_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** cancel — Hủy worker khi user đăng xuất. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("greenhouse_alert_worker")
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  doWork — Entry point của Worker, chạy trên IO dispatcher (suspend)
    // ─────────────────────────────────────────────────────────────────────
    override suspend fun doWork(): Result {
        return try {
            // 1. Kiểm tra user đã đăng nhập chưa
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.success()  // Không có user → skip

            // 2. Đọc dữ liệu sensor mới nhất từ Firebase
            val sensorSnap = db.child("sensors").get().await()
            val temperature = sensorSnap.child("temperature").getValue(Double::class.java)
                ?: return Result.success()  // Không có sensor data → skip
            val humidity = sensorSnap.child("humidity").getValue(Double::class.java)
                ?: return Result.success()

            // 3. Đọc ngưỡng: ưu tiên customThresholds, fallback về plant profile
            val (tempMin, tempMax, humidMin, humidMax) = loadThresholds(uid)

            // 4. Kiểm tra và gửi cảnh báo
            checkAndAlert(uid, temperature, humidity, tempMin, tempMax, humidMin, humidMax)

            Result.success()

        } catch (e: Exception) {
            // Lỗi mạng hoặc Firebase → retry sau
            Result.retry()
        }
    }

    /**
     * loadThresholds — Đọc ngưỡng theo thứ tự ưu tiên:
     * 1. customThresholds của user (nếu đã tùy chỉnh trong ThresholdFragment)
     * 2. Ngưỡng mặc định từ plantProfile (cây đang chọn)
     * 3. Hardcode fallback nếu không có gì
     *
     * Returns: Quadruple(tempMin, tempMax, humidMin, humidMax)
     */
    private suspend fun loadThresholds(uid: String): List<Double> {
        val userSnap = db.child("users").child(uid).get().await()
        val customSnap = userSnap.child("customThresholds")

        return if (customSnap.exists()) {
            // Đọc customThresholds (đã set bởi ThresholdFragment → saveThresholds())
            listOf(
                customSnap.child("tempMin").getValue(Double::class.java) ?: 15.0,
                customSnap.child("tempMax").getValue(Double::class.java) ?: 35.0,
                customSnap.child("humidityMin").getValue(Double::class.java) ?: 40.0,
                customSnap.child("humidityMax").getValue(Double::class.java) ?: 90.0
            )
        } else {
            // Fallback: đọc ngưỡng từ plant profile
            val plantId = userSnap.child("selectedPlant")
                .getValue(String::class.java) ?: "TOMATO"
            val plantSnap = db.child("plantProfiles").child(plantId).get().await()
            listOf(
                plantSnap.child("tempMin").getValue(Double::class.java) ?: 15.0,
                plantSnap.child("tempMax").getValue(Double::class.java) ?: 35.0,
                plantSnap.child("humidityMin").getValue(Double::class.java) ?: 40.0,
                plantSnap.child("humidityMax").getValue(Double::class.java) ?: 90.0
            )
        }
    }

    /**
     * checkAndAlert — So sánh giá trị sensor với ngưỡng.
     * Với mỗi trường hợp vượt ngưỡng:
     * 1. Kiểm tra cooldown (chống spam)
     * 2. Ghi entry vào Firebase notifications/$uid
     * 3. Trigger local notification
     */
    private suspend fun checkAndAlert(
        uid: String,
        temperature: Double, humidity: Double,
        tempMin: Double, tempMax: Double,
        humidMin: Double, humidMax: Double
    ) {
        // ── Kiểm tra nhiệt độ ──────────────────────────────────────────
        when {
            temperature > tempMax -> sendAlert(
                uid        = uid,
                alertKey   = "TEMP_HIGH",
                title      = "⚠️ Nhiệt độ quá cao!",
                message    = "Nhiệt độ ${String.format("%.1f", temperature)}°C vượt ngưỡng (>${tempMax.toInt()}°C)",
                type       = "TEMPERATURE",
                sensorVal  = temperature
            )
            temperature < tempMin -> sendAlert(
                uid        = uid,
                alertKey   = "TEMP_LOW",
                title      = "⚠️ Nhiệt độ quá thấp!",
                message    = "Nhiệt độ ${String.format("%.1f", temperature)}°C thấp hơn ngưỡng (<${tempMin.toInt()}°C)",
                type       = "TEMPERATURE",
                sensorVal  = temperature
            )
        }

        // ── Kiểm tra độ ẩm ────────────────────────────────────────────
        when {
            humidity > humidMax -> sendAlert(
                uid        = uid,
                alertKey   = "HUMID_HIGH",
                title      = "⚠️ Độ ẩm quá cao!",
                message    = "Độ ẩm ${String.format("%.1f", humidity)}% vượt ngưỡng (>${humidMax.toInt()}%)",
                type       = "HUMIDITY",
                sensorVal  = humidity
            )
            humidity < humidMin -> sendAlert(
                uid        = uid,
                alertKey   = "HUMID_LOW",
                title      = "⚠️ Độ ẩm quá thấp!",
                message    = "Độ ẩm ${String.format("%.1f", humidity)}% thấp hơn ngưỡng (<${humidMin.toInt()}%)",
                type       = "HUMIDITY",
                sensorVal  = humidity
            )
        }
    }

    /**
     * sendAlert — Ghi thông báo vào Firebase và trigger local notification.
     *
     * Cooldown check: Nếu cùng loại alertKey đã gửi trong [cooldownMs] → skip.
     * Điều này tránh spam khi sensor liên tục ở mức cảnh báo.
     *
     * Firebase path: GreenHouseSystem/notifications/$uid/$newKey
     * NotificationViewModel đọc path này qua ValueEventListener.
     */
    private suspend fun sendAlert(
        uid: String, alertKey: String,
        title: String, message: String,
        type: String, sensorVal: Double
    ) {
        // Kiểm tra cooldown
        val lastSentTime = prefs.getLong(alertKey, 0L)
        val now = System.currentTimeMillis()
        if (now - lastSentTime < cooldownMs) return  // Còn trong cooldown → skip

        // Cập nhật thời gian gửi mới nhất
        prefs.edit().putLong(alertKey, now).apply()

        // Ghi vào Firebase notifications/$uid (NotificationViewModel sẽ đọc)
        val ref = db.child("notifications").child(uid).push()
        val data = mapOf(
            "id"          to (ref.key ?: ""),
            "title"       to title,
            "message"     to message,
            "type"        to type,
            "timestamp"   to now,
            "isRead"      to false,
            "sensorValue" to sensorVal  // Giá trị số để hiện trong NotificationAdapter
        )
        ref.setValue(data).await()

        // Trigger local notification để user thấy kể cả khi app đóng
        showLocalNotification(title, message, type)
    }

    /**
     * showLocalNotification — Hiện Android notification trên status bar.
     * Intent mở app và navigate đến NotificationFragment khi user tap.
     */
    private fun showLocalNotification(title: String, message: String, type: String) {
        ensureNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Màu accent theo loại cảnh báo
        val accentColor = when (type) {
            "TEMPERATURE" -> 0xFFEF5350.toInt()
            "HUMIDITY"    -> 0xFF42A5F5.toInt()
            else          -> 0xFFCCFF00.toInt()
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(accentColor)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            // Heads-up notification (hiện popup ngay cả khi đang dùng app khác)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Dùng type.hashCode() làm notification ID → mỗi loại có 1 notification riêng
        manager.notify(type.hashCode(), notification)
    }

    /** ensureNotificationChannel — Tạo channel cho Android O+ nếu chưa có. */
    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo nhiệt độ và độ ẩm nhà kính"
                enableLights(true)
                lightColor  = 0xCCFF00  // Lime LED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
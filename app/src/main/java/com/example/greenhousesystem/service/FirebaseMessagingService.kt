package com.example.greenhousesystem.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.greenhousesystem.MainActivity
import com.example.greenhousesystem.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GreenHouseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "greenhouse_alerts"
        const val CHANNEL_NAME = "Cảnh báo nhà kính"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Lưu FCM token mới lên Firebase
        saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "GreenHouse Alert"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        val type = message.data["type"] ?: "TEMPERATURE"

        // Lưu thông báo vào Firebase
        saveNotificationToDatabase(title, body, type)

        // Hiện notification
        showNotification(title, body, type)
    }

    private fun saveFcmToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("GreenHouseSystem")
            .child("users")
            .child(uid)
            .child("fcmToken")
            .setValue(token)
    }

    private fun saveNotificationToDatabase(
        title: String,
        body: String,
        type: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference
            .child("GreenHouseSystem")
            .child("notifications")
            .child(uid)
            .push()

        val data = mapOf(
            "id" to (ref.key ?: ""),
            "title" to title,
            "message" to body,
            "type" to type,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        ref.setValue(data)
    }

    private fun showNotification(title: String, body: String, type: String) {
        createNotificationChannel()

        // Icon theo type
        val icon = when (type) {
            "TEMPERATURE" -> R.drawable.ic_warning
            "HUMIDITY" -> R.drawable.ic_warning
            else -> R.drawable.ic_greenhouse
        }

        // Màu theo type
        val color = when (type) {
            "TEMPERATURE" -> 0xFFE53935.toInt()
            "HUMIDITY" -> 0xFF1E88E5.toInt()
            else -> 0xFF2E7D32.toInt()
        }

        // Intent mở app khi nhấn thông báo
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(color)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo nhiệt độ và độ ẩm nhà kính"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
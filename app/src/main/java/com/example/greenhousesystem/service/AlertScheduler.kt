package com.example.greenhousesystem.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AlertScheduler {

    private const val ALERT_WORK_NAME = "greenhouse_alert_worker"
    private const val HISTORY_WORK_NAME = "greenhouse_history_worker"


    fun start(context: Context) {
        startAlertWorker(context)
        startHistoryWorker(context)
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ALERT_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(HISTORY_WORK_NAME)
    }

    private fun startAlertWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AlertWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ALERT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun startHistoryWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Lưu lịch sử mỗi 15 phút
        val workRequest = PeriodicWorkRequestBuilder<SensorHistoryWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HISTORY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


}
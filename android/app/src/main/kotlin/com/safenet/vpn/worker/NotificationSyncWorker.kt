package com.safenet.vpn.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safenet.vpn.MainActivity
import com.safenet.vpn.R
import com.safenet.vpn.core.security.TokenManager
import com.safenet.vpn.data.remote.SafeNetApiService
import com.safenet.vpn.data.remote.dto.SafeNetNotificationDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: SafeNetApiService,
    private val tokenManager: TokenManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (tokenManager.getAccessToken().isNullOrBlank()) return Result.success()
        if (!canPostNotifications()) return Result.success()

        return runCatching {
            val response = apiService.getMyNotifications(
                after = tokenManager.getLastNotificationSync(),
                limit = 20,
            )
            if (response.code() == 401 || response.code() == 403) return Result.success()
            if (response.code() == 429) return Result.retry()
            if (!response.isSuccessful || response.body()?.success != true) return Result.success()

            val notifications = response.body()?.data.orEmpty()
            notifications.asReversed().forEach { notification ->
                if (!notification.isRead) {
                    showNotification(notification)
                }
            }

            notifications.maxByOrNull { it.createdAt }?.createdAt?.let {
                tokenManager.saveLastNotificationSync(it)
            }

            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun showNotification(notification: SafeNetNotificationDto) {
        val channelId = "safenet_notification_center"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "SafeNet Notifications",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "SafeNet VPS notification center alerts"
                },
            )
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("open_notifications", true)
            .putExtra("notification_id", notification.id)
            .putExtra("notification_type", notification.type ?: notification.data?.get("type") ?: "broadcast")

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val localNotification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_safenet_logo)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notification.id.hashCode(), localNotification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }
}

package com.hevin.pushscreen.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hevin.pushscreen.AppApplication
import com.hevin.pushscreen.R

object NotificationHelper {
    private const val CHANNEL_ID = "com.hevin.pushscreen.NOTIFICATION_CHANNEL_1"
    private const val NOTIFICATION_ID = 10
    private val applicationContext = AppApplication.context

    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showForegroundNotification(service: Service) {
        // 创建 Notification Channel
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        service.startForeground(NOTIFICATION_ID, notification)
    }
}
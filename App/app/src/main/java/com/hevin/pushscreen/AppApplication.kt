package com.hevin.pushscreen

import android.app.Application
import android.content.Context
import com.hevin.pushscreen.state.AppStore
import com.hevin.pushscreen.utils.NotificationHelper

class AppApplication : Application() {

    companion object {
        val appStore = AppStore()
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        NotificationHelper.createNotificationChannel()
    }
}
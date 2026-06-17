package com.safenet.vpn

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.safenet.vpn.worker.NotificationSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * SafeNet VPN Application class.
 * Initializes Hilt and WorkManager with Hilt support.
 */
@HiltAndroidApp
class SafeNetApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        NotificationSyncScheduler.enqueueImmediate(this)
        NotificationSyncScheduler.schedulePeriodic(this)
    }

    // WorkManager configuration with Hilt worker factory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

}

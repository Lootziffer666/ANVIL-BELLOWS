package com.anvil.bellows.util

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AnvilApiService : Service() {

    @Inject lateinit var server: AnvilApiServer

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        server.startServer()
        return START_STICKY
    }

    override fun onDestroy() {
        server.stopServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "anvil_api"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Anvil API", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Local OpenAI-compatible proxy on port 4141" }
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Anvil API active")
            .setContentText("OpenAI-compatible proxy running on port 4141")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1337
    }
}

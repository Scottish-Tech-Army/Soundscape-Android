package com.kersnazzle.soundscapealpha.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.kersnazzle.soundscapealpha.R

// Attempting to wrap the FusedLocation API in a foreground service
class LocationService: Service() {
    // use FusedLocation API
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Binder object for local communication with client activities
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        // reference for the clients of this service
        fun getService(): LocationService = this@LocationService
    }

    // client activities can obtain the binder and call the service methods
    // start, stop
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //Have we started?
        Log.d("${LocationService::class.simpleName}", "onStartCommand")
        // Yarp. Now create a foreground service
        startForeground()

        return START_STICKY
    }

    private fun startForeground() {
        Log.d("--- ${LocationService::class.simpleName}", "Start Foreground")
        // Notify user that we are running a foreground service so they know we aren't
        // doing anything naughty other than spanking their battery

        ServiceCompat.startForeground(
            this,
            Companion.NOTIFICATION_ID,
            getNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    private fun getNotification(): Notification {
        createServiceNotificationChannel()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            // some sort of Soundscape icon thingy here
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)

        return builder.build()
    }

    private fun createServiceNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        // erm, not sure what this should be but not 0 apparently
        private const val NOTIFICATION_ID = 1000000
        private const val CHANNEL_ID = "channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "SoundscapeAlpha"

    }


}
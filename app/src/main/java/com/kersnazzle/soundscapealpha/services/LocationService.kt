package com.kersnazzle.soundscapealpha.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

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

        return START_STICKY
    }


}
package com.kersnazzle.soundscapealpha.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

// Attempting to wrap the FusedLocation API in a foreground service
class LocationService: Service() {
    // Binder object for local communication with client activities
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        // reference for the clients of this service
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
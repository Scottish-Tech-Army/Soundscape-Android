package com.kersnazzle.soundscapealpha.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.IBinder

// Attempting to wrap the FusedLocation API in a foreground service
class LocationService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
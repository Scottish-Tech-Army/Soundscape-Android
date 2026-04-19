package org.scottishtecharmy.soundscape.screens.onboarding.battery

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri

// BatteryOptimization composable is now in shared module

@Composable
fun BatteryOptimizationScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BatteryOptimization(
        onContinue = {
            requestBatteryOptimizationExemption(context)
            onNavigate()
        },
        modifier = modifier,
    )
}

@SuppressLint("BatteryLife")
fun requestBatteryOptimizationExemption(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun BatteryOptimizationPreview() {
    BatteryOptimization(onContinue = {})
}

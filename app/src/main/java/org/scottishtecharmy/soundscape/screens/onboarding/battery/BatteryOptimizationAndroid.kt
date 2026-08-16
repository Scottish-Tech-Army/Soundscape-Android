package org.scottishtecharmy.soundscape.screens.onboarding.battery

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    val alreadyExempted = remember { isIgnoringBatteryOptimizations(context) }

    // Skip this screen entirely if the exemption is already granted (e.g. the
    // user re-runs onboarding after already having granted it).
    LaunchedEffect(Unit) {
        if (alreadyExempted) {
            onNavigate()
        }
    }

    // Android doesn't report whether the user tapped Allow or Deny in the system
    // dialog - either way there's nothing more to ask, so move straight on to the
    // next screen once it returns, rather than showing a separate Continue button.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        onNavigate()
    }

    // Always render real content, even on the frame before the LaunchedEffect above
    // navigates away - swiping back into this screen from Listening re-enters this
    // composable, and if it rendered nothing while alreadyExempted, the predictive
    // back animation showed a blank/grey frame instead of this screen's background.
    BatteryOptimization(
        onContinue = onNavigate,
        modifier = modifier,
        onGrantPermission = if (alreadyExempted) {
            null
        } else {
            { launcher.launch(batteryOptimizationExemptionIntent(context)) }
        },
    )
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

// No FLAG_ACTIVITY_NEW_TASK here: this is launched via an ActivityResultLauncher,
// and that flag prevents the result being delivered back to the launcher.
@SuppressLint("BatteryLife")
private fun batteryOptimizationExemptionIntent(context: Context) =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }

@SuppressLint("BatteryLife")
fun requestBatteryOptimizationExemption(context: Context) {
    if (!isIgnoringBatteryOptimizations(context)) {
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

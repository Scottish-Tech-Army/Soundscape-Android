package org.scottishtecharmy.soundscape.xr

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi

@OptIn(ExperimentalProjectedApi::class)
object SoundscapeXR {
    fun launchXR(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val options = ProjectedContext.createProjectedActivityOptions(context)
                val intent = Intent(context, GlassesMainActivity::class.java)
                context.startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                // Fallback or log error
                val intent = Intent(context, GlassesMainActivity::class.java)
                context.startActivity(intent)
            }
        } else {
            // Fallback for older devices (though activity won't be projected)
            val intent = Intent(context, GlassesMainActivity::class.java)
            context.startActivity(intent)
        }
    }
}

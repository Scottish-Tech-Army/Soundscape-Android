package org.scottishtecharmy.soundscape.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue
import kotlin.concurrent.Volatile

@OptIn(ExperimentalForeignApi::class)
class IosNetworkUtils {
    // Optimistic at startup so requests aren't gated to cache-only before NWPathMonitor's
    // first callback fires.
    @Volatile
    private var connected: Boolean = true

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            connected = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_set_queue(
            monitor,
            dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
        )
        nw_path_monitor_start(monitor)
    }

    fun hasNetwork(): Boolean = connected
}

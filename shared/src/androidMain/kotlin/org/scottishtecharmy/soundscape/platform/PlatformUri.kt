package org.scottishtecharmy.soundscape.platform

import android.net.Uri

actual fun urlEncode(s: String): String = Uri.encode(s)
actual fun urlDecode(s: String): String = Uri.decode(s)

package org.scottishtecharmy.soundscape.i18n

import android.content.Context
import org.scottishtecharmy.soundscape.R

class AndroidLocalizedStrings(private val context: Context) : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        context.getString(resId(key), *args)

    override fun getOrNull(key: StringKey, vararg args: Any?): String? =
        runCatching { get(key, *args) }.getOrNull()

    private fun resId(key: StringKey): Int = when (key) {
        StringKey.ConfectNameTo      -> R.string.confect_name_to
        StringKey.ConfectNameToVia   -> R.string.confect_name_to_via
        StringKey.ConfectNameVia     -> R.string.confect_name_via
        StringKey.ConfectNameJoins   -> R.string.confect_name_joins
        StringKey.ConfectNameDeadEnd -> R.string.confect_name_dead_end
    }
}

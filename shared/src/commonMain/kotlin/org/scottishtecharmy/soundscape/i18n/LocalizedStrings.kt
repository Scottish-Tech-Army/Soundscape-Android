package org.scottishtecharmy.soundscape.i18n

interface LocalizedStrings {
    fun get(key: StringKey, vararg args: Any?): String
    fun getOrNull(key: StringKey, vararg args: Any?): String?
}

enum class StringKey {
    ConfectNameTo,
    ConfectNameToVia,
    ConfectNameVia,
    ConfectNameJoins,
    ConfectNameDeadEnd,
    ConfectNamePavementNextTo,
    ConfectNamePavement,
}

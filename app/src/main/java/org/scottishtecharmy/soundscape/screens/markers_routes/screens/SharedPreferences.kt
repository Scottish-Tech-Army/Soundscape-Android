package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import android.content.Context
import androidx.core.content.edit

// SharedPreferences Helper Functions
fun saveSortOrderPreference(context: Context, isSortAscending: Boolean) {
    val sharedPreferences = context.getSharedPreferences("SORT_ORDER_PREFS", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("SORT_ORDER_KEY", isSortAscending)
    }
}
fun saveSortFieldPreference(context: Context, isSortByName: Boolean) {
    val sharedPreferences = context.getSharedPreferences("SORT_FIELD_PREFS", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("SORT_FIELD_KEY", isSortByName)
    }
}

fun getSortOrderPreference(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("SORT_ORDER_PREFS", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("SORT_ORDER_KEY", true) // Default to false (ascending)
}

fun getSortFieldPreference(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("SORT_FIELD_PREFS", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("SORT_FIELD_PREFS", false) // Default to false (distance)
}
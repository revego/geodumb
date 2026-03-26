package com.code4you.geodumb

import android.content.Context

object PrefsManager {

    private const val PREF_APP = "app_prefs"
    private const val KEY_REPORTS_LIMIT = "reports_limit"

    fun getReportLimit(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REPORTS_LIMIT, 2)
    }

    fun setReportLimit(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REPORTS_LIMIT, value).apply()
    }
}
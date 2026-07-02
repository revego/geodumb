package com.code4you.geodumb

import android.content.Context

object PrefsManager {

    private const val PREF_APP = "app_prefs"
    private const val KEY_REPORTS_LIMIT = "reports_limit"
    private const val KEY_REPORTS_SENT = "reports_sent"  // <-- AGGIUNGI

    fun getReportLimit(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REPORTS_LIMIT, 0)
    }

    fun setReportLimit(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REPORTS_LIMIT, value).apply()
    }

    // AGGIUNGI QUESTE FUNZIONI
    fun getReportsSent(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REPORTS_SENT, 0)
    }

    fun setReportsSent(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REPORTS_SENT, value).apply()
    }
}
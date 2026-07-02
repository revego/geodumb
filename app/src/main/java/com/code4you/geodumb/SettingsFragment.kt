package com.code4you.geodumb

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.code4you.geodumb.api.RetrofitClient
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var switchEnableReports: Switch
    private lateinit var switchEnableCity: Switch
    private lateinit var textCity: TextView

    override fun onResume() {
        super.onResume()
        updateReportCounter()
        updateReportUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchEnableReports = view.findViewById(R.id.switch_enable_reports)
        switchEnableCity = view.findViewById(R.id.switch_city_filter)
        textCity = view.findViewById(R.id.text_city)

        // Disabilita subito entrambi gli switch (non interattivi)
        switchEnableReports.isEnabled = false
        switchEnableCity.isEnabled = false

        // Applica subito lo stato locale (dati già salvati)
        updateSwitchesState()

        val spinnerTheme = view.findViewById<Spinner>(R.id.spinner_theme)
        val themes = listOf("Sistema")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, themes)
        spinnerTheme.adapter = adapter

        // Aggiornamento da server (poi richiamerà nuovamente updateSwitchesState)
        fetchAndApplyRateLimit()
    }

    // ✅ Calcola se l'utente può ancora inviare segnalazioni e aggiorna entrambi gli switch
    private fun updateSwitchesState() {
        val limit = PrefsManager.getReportLimit(requireContext())
        val sent = requireContext()
            .getSharedPreferences("app_prefs", 0)
            .getInt("reports_sent", 0)

        val canSend = sent < limit   // true solo se ci sono report ancora disponibili

        applyRateLimitToSwitch(canSend)
        applyCityFilterSwitchState(canSend)
    }

    private fun applyRateLimitToSwitch(canSend: Boolean) {
        switchEnableReports.isEnabled = false
        switchEnableReports.isChecked = canSend

        val thumbColor = if (canSend) Color.GREEN else Color.RED
        val trackColor = if (canSend) Color.parseColor("#88FF88") else Color.parseColor("#FF8888")
        switchEnableReports.thumbDrawable?.setTint(thumbColor)
        switchEnableReports.trackDrawable?.setTint(trackColor)
    }

    private fun applyCityFilterSwitchState(canSend: Boolean) {
        switchEnableCity.isEnabled = false
        switchEnableCity.isChecked = canSend

        val thumbColor = if (canSend) Color.GREEN else Color.RED
        switchEnableCity.thumbDrawable?.setTint(thumbColor)

        textCity.alpha = if (canSend) 1.0f else 0.4f
    }

    private fun fetchAndApplyRateLimit() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getRateLimit()
                PrefsManager.setReportLimit(requireContext(), response.count)

                // ✅ SINCRONIZZA IL CONTATORE LOCALE
                val prefs = requireContext().getSharedPreferences("app_prefs", 0)
                prefs.edit().putInt("reports_sent", response.sent).apply()

                updateSwitchesState()   // ora canSend = response.sent < response.count
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Errore recupero rate limit", e)
                updateSwitchesState()
            }
        }
    }

    private fun fetchAndApplyRateLimit_() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getRateLimit()
                val count = response.count   // count è il limite massimo

                PrefsManager.setReportLimit(requireContext(), count)
                // Aggiorna gli switch con la nuova disponibilità
                updateSwitchesState()
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Errore recupero rate limit", e)
                // In caso di errore usa comunque i dati locali (già aggiornati all'apertura)
                updateSwitchesState()
            }
        }
    }

    // ✅ INVARIATE le funzioni per i contatori testuali
    private fun updateReportCounter() {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        val sent = prefs.getInt("reports_sent", 0)
        val limit = PrefsManager.getReportLimit(requireContext())
        val text = view?.findViewById<TextView>(R.id.text_report_limit)

        text?.text = if (sent >= limit) {
            "Limite raggiunto ($sent / $limit)"
        } else {
            "Segnalazioni: $sent / $limit"
        }
    }

    private fun updateReportUI() {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        val sent = prefs.getInt("reports_sent", 0)
        val limit = PrefsManager.getReportLimit(requireContext())
        val text = view?.findViewById<TextView>(R.id.text_report_limit)

        text?.text = if (sent >= limit) {
            "Hai raggiunto il limite ($sent/$limit).\nAttendi lo sblocco o nuove disponibilità."
        } else {
            "Segnalazioni disponibili: ${limit - sent}"
        }
    }
}